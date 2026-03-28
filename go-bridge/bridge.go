/*
 * Copyright (c) 2026 SMH01
 */

package main

import "C"
import (
	"encoding/base64"
	"fmt"
	"strings"
	"sync"

	"github.com/amnezia-vpn/amneziawg-go/conn"
	"github.com/amnezia-vpn/amneziawg-go/device"
	"github.com/amnezia-vpn/amneziawg-go/tun"
	"github.com/ProtonMail/go-srp"
	"github.com/ProtonVPN/go-vpn-lib/ed25519"
	"golang.org/x/crypto/curve25519"
)

var (
	vpnDevice *device.Device
	vpnMutex  sync.Mutex
)

//export SRPCompute
func SRPCompute(username, password, b64salt, signedModulus, serverEphemeral *C.char, version C.int) *C.char {
	u := C.GoString(username)
	p := C.GoString(password)
	s := C.GoString(b64salt)
	m := C.GoString(signedModulus)
	e := C.GoString(serverEphemeral)

	auth, err := srp.NewAuth(int(version), u, []byte(p), s, m, e)
	if err != nil {
		return C.CString(fmt.Sprintf("ERROR: NewAuth failed: %v", err))
	}

	// Proton uses 2048 bit SRP
	proofs, err := auth.GenerateProofs(2048)
	if err != nil {
		return C.CString(fmt.Sprintf("ERROR: GenerateProofs failed: %v", err))
	}

	clientEphemeral := base64.StdEncoding.EncodeToString(proofs.ClientEphemeral)
	clientProof := base64.StdEncoding.EncodeToString(proofs.ClientProof)

	// Return space separated base64 proofs: ClientEphemeral ClientProof
	return C.CString(fmt.Sprintf("%s %s", clientEphemeral, clientProof))
}

//export GenerateWGKeys
func GenerateWGKeys() *C.char {
	kp, err := ed25519.NewKeyPair()
	if err != nil {
		return C.CString(fmt.Sprintf("ERROR: %v", err))
	}

	// X25519 Private Key for the tunnel
	priv := kp.ToX25519()
	privateKeyB64 := base64.StdEncoding.EncodeToString(priv)

	// Ed25519 Public Key in PEM format for the API registration
	publicPem, err := kp.PublicKeyPKIXPem()
	if err != nil {
		return C.CString(fmt.Sprintf("ERROR: %v", err))
	}

	// X25519 Public Key for the tunnel (optional but consistent)
	pub, err := curve25519.X25519(priv, curve25519.Basepoint)
	if err != nil {
		return C.CString(fmt.Sprintf("ERROR: %v", err))
	}
	publicKeyB64 := base64.StdEncoding.EncodeToString(pub)

	// Return: X25519_PRIV_B64 ED25519_PUB_PEM_B64 X25519_PUB_B64
	// We encode PEM in base64 to avoid space/newline issues in the split
	publicPemB64 := base64.StdEncoding.EncodeToString([]byte(publicPem))

	return C.CString(fmt.Sprintf("%s %s %s", privateKeyB64, publicPemB64, publicKeyB64))
}

//export ConnectVpn
func ConnectVpn(interfaceName *C.char, config *C.char) *C.char {
	vpnMutex.Lock()
	defer vpnMutex.Unlock()

	if vpnDevice != nil {
		return C.CString("ERROR: Already connected")
	}

	name := C.GoString(interfaceName)
	conf := C.GoString(config)

	// Create TUN device
	tdev, err := tun.CreateTUN(name, device.DefaultMTU)
	if err != nil {
		return C.CString(fmt.Sprintf("ERROR: Failed to create TUN: %v", err))
	}

	logger := device.NewLogger(device.LogLevelError, fmt.Sprintf("(%s) ", name))
	dev := device.NewDevice(tdev, conn.NewDefaultBind(), logger)

	// Apply configuration
	err = dev.IpcSet(conf)
	if err != nil {
		dev.Close()
		return C.CString(fmt.Sprintf("ERROR: Failed to set config: %v", err))
	}

	err = dev.Up()
	if err != nil {
		dev.Close()
		return C.CString(fmt.Sprintf("ERROR: Failed to bring up device: %v", err))
	}

	vpnDevice = dev

	return C.CString("SUCCESS")
}

//export DisconnectVpn
func DisconnectVpn() *C.char {
	vpnMutex.Lock()
	defer vpnMutex.Unlock()

	if vpnDevice == nil {
		return C.CString("ERROR: Not connected")
	}

	vpnDevice.Close()
	vpnDevice = nil

	return C.CString("SUCCESS")
}

//export GetVpnStats
func GetVpnStats() *C.char {
	vpnMutex.Lock()
	defer vpnMutex.Unlock()

	if vpnDevice == nil {
		return C.CString("0 0") // rx tx
	}

	uapi, err := vpnDevice.IpcGet()
	if err != nil {
		return C.CString("0 0")
	}

	var rx, tx int64
	lines := strings.Split(uapi, "\n")
	for _, line := range lines {
		if strings.HasPrefix(line, "rx_bytes=") {
			fmt.Sscanf(line, "rx_bytes=%d", &rx)
		} else if strings.HasPrefix(line, "tx_bytes=") {
			fmt.Sscanf(line, "tx_bytes=%d", &tx)
		}
	}

	return C.CString(fmt.Sprintf("%d %d", rx, tx))
}

func main() {}
