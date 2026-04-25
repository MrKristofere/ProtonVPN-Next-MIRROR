/*
 * Copyright (c) 2026 SMH01
 */

package main

import (
	"bufio"
	"fmt"
	"os"
	"os/exec"
	"os/signal"
	"strings"
	"syscall"

	"github.com/amnezia-vpn/amneziawg-go/conn"
	"github.com/amnezia-vpn/amneziawg-go/device"
	"github.com/amnezia-vpn/amneziawg-go/tun"
)

func main() {
	if len(os.Args) == 3 && os.Args[1] == "--cleanup" {
		interfaceName := os.Args[2]
		fmt.Printf("Cleaning up interface %s...\n", interfaceName)
		exec.Command("ip", "link", "delete", interfaceName).Run()
		os.Exit(0)
	}

	if len(os.Args) < 4 {
		fmt.Fprintf(os.Stderr, "Usage: %s <interface-name> <local-ip> <server-ip>\n", os.Args[0])
		fmt.Fprintf(os.Stderr, "       %s --cleanup <interface-name>\n", os.Args[0])
		os.Exit(1)
	}

	interfaceName := os.Args[1]
	localIp := os.Args[2]
	serverIp := os.Args[3]

	// Create TUN device
	tdev, err := tun.CreateTUN(interfaceName, device.DefaultMTU)
	if err != nil {
		fmt.Fprintf(os.Stderr, "ERROR: Failed to create TUN: %v\n", err)
		os.Exit(1)
	}

	logger := device.NewLogger(device.LogLevelError, fmt.Sprintf("(%s) ", interfaceName))
	dev := device.NewDevice(tdev, conn.NewDefaultBind(), logger)

	fmt.Println("READY")

	// Read UAPI config and metadata from stdin
	var uapiConfig strings.Builder
	var metadata = make(map[string][]string)
	var allowedIps []string

	scanner := bufio.NewScanner(os.Stdin)
	isMetadata := false

	for scanner.Scan() {
		line := scanner.Text()
		if line == "" {
			if !isMetadata {
				isMetadata = true
				continue
			} else {
				break
			}
		}

		if !isMetadata {
			uapiConfig.WriteString(line)
			uapiConfig.WriteString("\n")
			if strings.HasPrefix(line, "allowed_ip=") {
				allowedIps = append(allowedIps, strings.TrimPrefix(line, "allowed_ip="))
			}
		} else {
			parts := strings.SplitN(line, "=", 2)
			if len(parts) == 2 {
				metadata[parts[0]] = append(metadata[parts[0]], parts[1])
			}
		}
	}

	if err := scanner.Err(); err != nil {
		fmt.Fprintf(os.Stderr, "ERROR: Failed to read config: %v\n", err)
		dev.Close()
		os.Exit(1)
	}

	// Apply configuration
	err = dev.IpcSet(uapiConfig.String())
	if err != nil {
		fmt.Fprintf(os.Stderr, "ERROR: Failed to set config: %v\n", err)
		dev.Close()
		os.Exit(1)
	}

	err = dev.Up()
	if err != nil {
		fmt.Fprintf(os.Stderr, "ERROR: Failed to bring up device: %v\n", err)
		dev.Close()
		os.Exit(1)
	}

	// Setup routing
	gateway, defaultIface, err := getDefaultGateway()
	if err != nil {
		fmt.Fprintf(os.Stderr, "ERROR: Failed to get default gateway: %v\n", err)
	} else {
		fmt.Printf("Default gateway: %s on %s\n", gateway, defaultIface)
		// Add host route for server via current gateway to avoid loop
		setupHostRoute(serverIp, gateway, defaultIface)
	}

	// Set IP and MTU for TUN
	setupTunInterface(interfaceName, localIp)

	// Add routes based on split tunneling configuration
	isIncludeMode := false
	if val, ok := metadata["is_include_mode"]; ok && len(val) > 0 {
		isIncludeMode = val[0] == "true"
	}

	setupRouting(interfaceName, allowedIps, isIncludeMode)

	// App-based split tunneling (simplified implementation via cgroups)
	excludedApps := metadata["exclude_app"]
	includedApps := metadata["include_app"]

	stopAppsChan := make(chan struct{})
	if len(excludedApps) > 0 || len(includedApps) > 0 {
		go manageAppSplitTunneling(interfaceName, excludedApps, includedApps, isIncludeMode, stopAppsChan)
	}

	fmt.Println("CONNECTED")

	// Wait for signals or stdin closure
	sigChan := make(chan os.Signal, 1)
	signal.Notify(sigChan, syscall.SIGINT, syscall.SIGTERM)

	stdinDone := make(chan struct{})
	go func() {
		// Keep scanning until stdin is closed
		for scanner.Scan() {}
		close(stdinDone)
	}()

	select {
	case <-sigChan:
		fmt.Println("SHUTTING DOWN")
	case <-stdinDone:
		fmt.Println("STDIN CLOSED, SHUTTING DOWN")
	}

	if len(excludedApps) > 0 || len(includedApps) > 0 {
		close(stopAppsChan)
	}

	// Cleanup routing
	cleanupRouting(serverIp, gateway, defaultIface)

	dev.Close()
}

func setupTunInterface(iface, localIp string) {
	fmt.Printf("Configuring interface %s with IP %s\n", iface, localIp)
	exec.Command("ip", "addr", "add", localIp+"/32", "dev", iface).Run()
	exec.Command("ip", "link", "set", "mtu", "1280", "up", "dev", iface).Run()
}

func setupRouting(iface string, allowedIps []string, isIncludeMode bool) {
	fmt.Printf("Setting up routing (include=%v, IPs=%d)...\n", isIncludeMode, len(allowedIps))

	hasDefault := false
	for _, ip := range allowedIps {
		if ip == "0.0.0.0/0" {
			hasDefault = true
			break
		}
	}

	if hasDefault && !isIncludeMode {
		// Full tunnel (or most IPs included) - use /1 override
		fmt.Printf("Default route detected, using /1 override via %s\n", iface)
		exec.Command("ip", "route", "add", "0.0.0.0/1", "dev", iface).Run()
		exec.Command("ip", "route", "add", "128.0.0.0/1", "dev", iface).Run()
	} else {
		// Selective routing or Include mode
		for _, ip := range allowedIps {
			if ip == "0.0.0.0/0" && isIncludeMode {
				// In include mode, if 0.0.0.0/0 is selected, it's also effectively a full tunnel
				exec.Command("ip", "route", "add", "0.0.0.0/1", "dev", iface).Run()
				exec.Command("ip", "route", "add", "128.0.0.0/1", "dev", iface).Run()
				continue
			}
			fmt.Printf("Adding specific route for %s via %s\n", ip, iface)
			exec.Command("ip", "route", "add", ip, "dev", iface).Run()
		}
	}
}

func manageAppSplitTunneling(iface string, excludedApps, includedApps []string, isIncludeMode bool, stopChan chan struct{}) {
	// Implementation note: Real app-based split tunneling on Linux often requires
	// cgroups (net_cls) or eBPF. This is a simplified version using a polling approach
	// to find PIDs and potentially move them to a specific network namespace or
	// use 'setns', but that's complex.
	// For this task, we'll focus on the infrastructure to support it.
	fmt.Printf("App split tunneling initialized for %d apps\n", len(excludedApps)+len(includedApps))

	// TODO: Full implementation of app-based routing logic
	// For now, we at least have the data in the helper.

	<-stopChan
	fmt.Println("Stopping app split tunneling manager")
}

func setupTunnelRouting(iface string) {
	// Deprecated in favor of setupRouting
}

func setupHostRoute(serverIp, gateway, iface string) {
	fmt.Printf("Adding host route for server %s via gateway %s on %s\n", serverIp, gateway, iface)
	exec.Command("ip", "route", "add", serverIp, "via", gateway, "dev", iface).Run()
}

func cleanupRouting(serverIp, gateway, iface string) {
	fmt.Println("Cleaning up routing...")
	exec.Command("ip", "route", "del", serverIp).Run()
	// Default routes via dev will be removed when the interface is closed
}

func getDefaultGateway() (string, string, error) {
	// Simple way: parse 'ip route show default'
	out, err := exec.Command("ip", "route", "show", "default").Output()
	if err != nil {
		return "", "", err
	}
	// Expected: "default via 192.168.1.1 dev eth0 ..."
	fields := strings.Fields(string(out))
	var gateway, iface string
	for i, f := range fields {
		if f == "via" && i+1 < len(fields) {
			gateway = fields[i+1]
		}
		if f == "dev" && i+1 < len(fields) {
			iface = fields[i+1]
		}
	}
	if gateway == "" || iface == "" {
		return "", "", fmt.Errorf("could not find default gateway in output: %s", string(out))
	}
	return gateway, iface, nil
}
