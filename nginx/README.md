# HexaQuot Nginx and DNS

The server is `194.164.169.80`. Configure these DNS records before requesting
the certificates:

| Name | Type | Value |
| --- | --- | --- |
| `@` | `A` | `194.164.169.80` |
| `www` | `A` or `CNAME` | `194.164.169.80` or `hexaquot.it` |
| `staging` | `A` | `194.164.169.80` |

Install `hexaquot.it.conf` in `/etc/nginx/conf.d/`, validate it with
`nginx -t`, obtain the two certificates using the webroot
`/usr/share/nginx/html`, and reload Nginx. The production and staging upstreams
remain respectively `localhost:8083` and `localhost:8084`.
