module ru.protonmod.next/bridge

go 1.24.4

require (
	github.com/ProtonMail/go-srp v0.0.0
	github.com/ProtonVPN/go-vpn-lib v0.0.0-00010101000000-000000000000
	github.com/amnezia-vpn/amneziawg-go v0.0.0
	golang.org/x/crypto v0.42.0
)

require (
	github.com/ProtonMail/bcrypt v0.0.0-20210511135022-227b4adcab57 // indirect
	github.com/ProtonMail/go-crypto v0.0.0-20230321155629-9a39f2531310 // indirect
	github.com/cloudflare/circl v1.1.0 // indirect
	github.com/cronokirby/saferith v0.33.0 // indirect
	github.com/pkg/errors v0.9.1 // indirect
	golang.org/x/net v0.44.0 // indirect
	golang.org/x/sys v0.36.0 // indirect
	golang.zx2c4.com/wintun v0.0.0-20230126152724-0fa3db229ce2 // indirect
)

replace (
	github.com/ProtonMail/go-srp => ../go-srp
	github.com/ProtonVPN/go-vpn-lib => ../go-vpn-lib
	github.com/amnezia-vpn/amneziawg-go => ../amneziawg-go
)
