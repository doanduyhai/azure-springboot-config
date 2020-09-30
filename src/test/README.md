How to setup your Mac OS for local testing using the sample application
==

The difficulty with this library is that it is relying on the non-routable `169.254.169.254` IP address to fetch the Oauth2 token

Follow the below procedure to configure your Mac for testing locally
 
## 1. Add **169.254.169.254** as alias to localhost

```bash
sudo ifconfig lo0 169.254.169.254 alias
```

## 2. Create anchor file for port forwarding

```bash
sudo vim /etc/pf.anchors/azure.msi
```

File content: 

```
rdr on lo0 proto tcp from any to 169.254.169.254 port 80 -> 127.0.0.1 port 8080
```
 
## 3. Test the anchor file

```bash
sudo pfctl -vnf /etc/pf.anchors/azure.msi
```

## 4. Edit the /etc/pf.conf file to append

```
rdr-anchor "azure_msi"
...
...
load anchor "azure_msi" from "/etc/pf.anchors/azure.msi"
```

## 5. Create `server.py`

```python

import sys
from SimpleHTTPServer import SimpleHTTPRequestHandler
import BaseHTTPServer


def test(HandlerClass=SimpleHTTPRequestHandler,
         ServerClass=BaseHTTPServer.HTTPServer):

    protocol = "HTTP/1.0"
    host = ''
    port = 8000
    if len(sys.argv) > 1:
        arg = sys.argv[1]
        if ':' in arg:
            host, port = arg.split(':')
            port = int(port)
        else:
            try:
                port = int(sys.argv[1])
            except:
                host = sys.argv[1]

    server_address = (host, port)

    HandlerClass.protocol_version = protocol
    httpd = ServerClass(server_address, HandlerClass)

    sa = httpd.socket.getsockname()
    print "Serving HTTP on", sa[0], "port", sa[1], "..."
    httpd.serve_forever()


if __name__ == "__main__":
    test()
```

## 6. Create test `index.html`

```html
<html>
<body>
SUCCESS !!!
</body>
</html>
```

## 7. Start python test server on **127.0.0.1:8080**

```bash
python server.py 127.0.0.1:8080
```

## 8. Try with curl on MSI endpoint

```bash
curl "http://169.254.169.254"                       
                                                                        
<html>
<body>
SUCCESS !!!
</body>
</html>
```

## 9. Create a folder for mocked backend and then install json-server with NPM

```bash
npm install -g json-server
```

## 10. Create a file `db.json` with the following content


```json
{
  "tokens": {
    "token_type": "Bearer",
    "access_token": "xxx",
    "expires_on": "xxx"
  }
}
```

## 11. Create a `routes.json` file routes with the following content

```json
{
  "/metadata/identity/oauth2/token": "/tokens",
  "/metadata/identity/oauth2/token\\?api-version=:api": "/tokens",
  "/metadata/identity/oauth2/token\\?api-version=:api&resource=:resource": "/tokens",
  "/metadata/identity/oauth2/token\\?api-version=:api&resource=:resource&client_id=:clientId": "/tokens"
}
```

This file will redirect any request path `/metadata/identity/oauth2/token` with any query parameters combination to the `/tokens` path of **json-server**

## 12. Start the server 

```bash
json-server --host 127.0.0.1 --port 8080 --routes routes.json --watch db.json`
```

## 13. Retrieve access token for key vault with Azure client

```bash
az account get-access-token --resource https://vault.azure.net

{
  "accessToken": "xxx",
  "expiresOn": "2020-09-30 10:42:00.615018",
  "subscription": "xxx",
  "tenant": "xxx",
  "tokenType": "Bearer"
}

```

## 14. Update the `db.json` file with the real access token value and the expiredsOn date (in EPOCH format !!!!)


 