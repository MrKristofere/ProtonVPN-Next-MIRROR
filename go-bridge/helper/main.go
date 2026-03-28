/*
 * Copyright (c) 2026 SMH01
 */

package main

import (
	"bufio"
	"fmt"
	"os"
	"os/signal"
	"strings"
	"syscall"

	"github.com/amnezia-vpn/amneziawg-go/conn"
	"github.com/amnezia-vpn/amneziawg-go/device"
	"github.com/amnezia-vpn/amneziawg-go/tun"
)

func main() {
	if len(os.Args) < 2 {
		fmt.Fprintf(os.Stderr, "Usage: %s <interface-name>\n", os.Args[0])
		os.Exit(1)
	}

	interfaceName := os.Args[1]

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

	dev.Close()
}
