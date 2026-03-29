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

	// Read UAPI config from stdin until an empty line or EOF
	var sb strings.Builder
	scanner := bufio.NewScanner(os.Stdin)
	for scanner.Scan() {
		line := scanner.Text()
		if line == "" {
			break
		}
		sb.WriteString(line)
		sb.WriteString("\n")
	}

	if err := scanner.Err(); err != nil {
		fmt.Fprintf(os.Stderr, "ERROR: Failed to read config: %v\n", err)
		dev.Close()
		os.Exit(1)
	}

	// Apply configuration
	err = dev.IpcSet(sb.String())
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
		// Add host route for server via current gateway
		setupHostRoute(serverIp, gateway, defaultIface)
	}

	// Set IP and MTU for TUN
	setupTunInterface(interfaceName, localIp)

	// Add default routes via tunnel
	setupTunnelRouting(interfaceName)

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

	// Cleanup routing
	cleanupRouting(serverIp, gateway, defaultIface)

	dev.Close()
}

func setupTunInterface(iface, localIp string) {
	fmt.Printf("Configuring interface %s with IP %s\n", iface, localIp)
	exec.Command("ip", "addr", "add", localIp+"/32", "dev", iface).Run()
	exec.Command("ip", "link", "set", "mtu", "1280", "up", "dev", iface).Run()
}

func setupTunnelRouting(iface string) {
	fmt.Printf("Setting up default routes via %s\n", iface)
	// We use 0.0.0.0/1 and 128.0.0.0/1 to override the default route without deleting it
	exec.Command("ip", "route", "add", "0.0.0.0/1", "dev", iface).Run()
	exec.Command("ip", "route", "add", "128.0.0.0/1", "dev", iface).Run()
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
