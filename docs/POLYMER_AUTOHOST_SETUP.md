# Polymer AutoHost Setup (Phase 2.1)

This mod now integrates with Polymer resource-pack generation.
As of `artifacts` `1.7.5`, the published mod jar also bundles `polymer-autohost` and creates a default `config/polymer/auto-host.json` with `"enabled": true` when that file is missing.

To automatically deliver the generated resource pack to vanilla clients:

1. Start the server once with the current `artifacts` jar.
2. Confirm `config/polymer/auto-host.json` was created.
3. Confirm `"enabled": true` in that file.
4. Restart the server if you changed the config.

If you are running an older `artifacts` jar that does not bundle `polymer-autohost`, install the **packed/bundled** Polymer server build separately.

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

## MineKeep Setup

MineKeep masks some forwarded connection details and only allows server resource packs from a whitelist of domains. In that environment, Polymer's default `polymer:automatic` mode can advertise an unusable backend address such as a private IP with port `0`.

Use `polymer:http_server` with one of MineKeep's allowed HTTP ports and a `*.minekeep.dev` public address instead:

```json
{
  "enabled": true,
  "required": true,
  "type": "polymer:http_server",
  "settings": {
    "port": 2053,
    "external_address": "https://your-server-name.minekeep.dev:2053/"
  }
}
```

If players join through `your-server-name.minekeep.gg`, use the matching `your-server-name.minekeep.dev` host for the resource pack URL.

## This Mod's Polymer Resource Pack Integration

This mod already:

- adds its assets to Polymer's generated resource pack
- marks the resource pack as required for clients
- bundles Polymer AutoHost in the server jar
- creates a default enabled AutoHost config when it is missing

So once AutoHost is enabled, clients should receive the pack automatically and see custom artifact models/textures.

## Troubleshooting

- If `config/polymer/auto-host.json` does not exist, confirm the server is running a recent `artifacts` jar or install the correct bundled Polymer server jar.
- If `polymer/resource_pack.zip` is missing after startup, check the server log for Polymer AutoHost startup errors.
- If clients join but still see vanilla fallback items, confirm they accepted the server resource pack.
- You can also manually generate the pack with Polymer's commands and test delivery separately.
