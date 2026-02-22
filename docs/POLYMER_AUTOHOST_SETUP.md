# Polymer AutoHost Setup (Phase 2.1)

This mod now integrates with Polymer resource-pack generation.

To automatically deliver the generated resource pack to vanilla clients:

1. Install the **packed/all** build of `Polymer` on the server (not just `polymer-core`).
2. Start the server once.
3. Confirm `config/polymer/auto-host.json` was created.
4. Edit `config/polymer/auto-host.json` and set `"enabled": true`.
5. Restart the server.

## Basic Direct Server Setup

Minimal change in `config/polymer/auto-host.json`:

```json
{
  "enabled": true
}
```

Polymer AutoHost will generate and serve the resource pack automatically on server startup.

## Proxy / Reverse Proxy Setup

If the server is behind a proxy or you need clients to use a public address, set:

```json
{
  "enabled": true,
  "forced_address": "http://your-domain-or-ip:port"
}
```

Example:

```json
{
  "enabled": true,
  "forced_address": "http://server.example.net:25565"
}
```

## Custom HTTP Port Setup

If you want Polymer to host the pack on a separate HTTP port:

```json
{
  "enabled": true,
  "type": "polymer:http_server",
  "settings": {
    "port": 25567,
    "external_address": "http://your-domain-or-ip:25567/"
  }
}
```

Make sure the `external_address` is reachable by players.

## This Mod's Polymer Resource Pack Integration

This mod already:

- adds its assets to Polymer's generated resource pack
- marks the resource pack as required for clients

So once AutoHost is enabled, clients should receive the pack automatically and see custom artifact models/textures.

## Troubleshooting

- If `config/polymer/auto-host.json` does not exist, install the correct Polymer server jar (packed/all build).
- If clients join but still see vanilla fallback items, confirm they accepted the server resource pack.
- You can also manually generate the pack with Polymer's commands and test delivery separately.

