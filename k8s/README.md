# Core Utils - Kubernetes projected volume token utilities

A lightweight Java library providing support for working with Kubernetes projected volume tokens.

## Features

- Lightweight API to get Kubernetes projected volume tokens by audience;
- Cache tokens and re-read from file system only when they are changed;
- Customizable via SPI;
- Make OIDC discovery to Kubernetes OIDC provider and verify projected volume tokens from Kubernetes;
- Cache verification keys and refetch when KID from the token is not found in the cache;

## Installation

Add the dependency to your Maven project:

```xml

<dependency>
    <groupId>com.netcracker.cloud.security.core.utils</groupId>
    <artifactId>k8s-utils</artifactId>
    <version>1.0.0</version>
</dependency>
```

## TokenSource

### Audiences

`KubernetesTokenAudiences` class has pre-defined audiences for different purposes.

| Audience                              | Purpose                                              |
|---------------------------------------|------------------------------------------------------|
| `KubernetesTokenAudiences.NETCRACKER` | Requests to microservices withing the same namespace |
| `KubernetesTokenAudiences.DBAAS`      | Requests to DBaaS infra service                      |
| `KubernetesTokenAudiences.MAAS`       | Requests to MaaS infra service                       |

### How to obtain a token by audience? (KubernetesAudienceToken)

The `KubernetesTokenSource.getToken(audience)` method is used to obtain a Kubernetes projected volume token by audience.
Use predefined constants for the audience argument. They are declared in the `KubernetesTokenAudiences` class.
For example, to obtain a token with the `KubernetesTokenAudiences.NETRACKER` audience, follow these steps:

```java
import com.netcracker.cloud.security.core.utils.k8s.AudienceName;
import com.netcracker.cloud.security.core.utils.k8s.KubernetesAudienceToken;

import java.util.function.Supplier;

public class Service {

    public Data getData(Supplier<String> tokenSupplier) {
        return doRequestWithToken(tokenSupplier.get());
    }
}

class Controller {

    private Service service;

    public void someLogic() {
        service.getData(() -> KubernetesAudienceToken.getToken(AudienceName.NETRACKER));
    }
}
```

**Note:** Never store token values in variables or collections, and do not cache tokens. Tokens may expire!!!
Obtain token just before a request.

### How to obtain a token for Kubernetes API call? (KubernetesServiceAccountToken)

If you need to make a call to a Kubernetes API you need to obtain Kubernetes API token. To do that, use the
`KubernetesDefaultToken` class:

```java
import com.netcracker.cloud.security.core.utils.k8s.KubernetesServiceAccountToken;

import java.net.http.HttpRequest;
import java.util.function.Supplier;

public class KubernetesOidcRestClient {

    public KubernetesOidcRestClient(Supplier<String> tokenSupplier) {
        this.tokenSupplier = tokenSupplier;
    }

    private String doRequest(String url) {
        var request = HttpRequest.newBuilder()
                .setHeader("Authorization", tokenSupplier.get())
                .uri(url)
                .GET()
                .build();
    }
}

public class KubernetesTokenVerifier {

    public KubernetesTokenVerifier() {
        this.restClient = new KubernetesOidcRestClient(KubernetesServiceAccountToken::getToken);
    }
}

```

### Properties (Local Dev)

| property                                                           | value example      | default value                                 | description                                                                  |
|--------------------------------------------------------------------|--------------------|-----------------------------------------------|------------------------------------------------------------------------------|
| com.netcracker.cloud.security.kubernetes.tokens.dir                | /path/to/directory | /var/run/secrets/tokens                       | change the directory where tokens are located                                |
| com.netcracker.cloud.security.kubernetes.service.account.token.dir | /path/to/directory | /var/run/secrets/kubernetes.io/serviceaccount | change the directory where kubernetes service account token is located       |
| com.netcracker.cloud.security.kubernetes.tokens.polling.interval   | 2m                 | 1m                                            | change the interval of polling events from file system about token rotations |

### Custom TokenSource

`com.netcracker.cloud.security.core.utils.k8s.impl.WatchingTokenSource` is the current default implementation of the
`com.netcracker.cloud.security.core.utils.k8s.TokenSource` interface. There is an ability to override the
implementation,
by implementing your own class (which implements `TokenSource`) and putting its full name into
`resources/META_INF/services/com.netcracker.cloud.security.core.utils.k8s.TokenSource` file.

For example for class:

```java
package com.orgatination.source;

import com.netcracker.cloud.security.core.utils.k8s.Priority;

@Priority(10)
public class CustomTokenSource {

    public String getToken(String audience) {
        return "test-token";
    }
}
```

Place following in `resources/META_INF/services/com.netcracker.cloud.security.core.utils.k8s.TokenSource`:

```text
com.orgatination.source.CustomTokenSource
```

Note: A project can have multiple `TokenSource` implementations; the library only selects the one with the highest
priority (the value of the `Priority` annotation).

## KubernetesTokenVerifier

### Verify tokens

To authenticate incoming requests that use Kubernetes projected volume tokens use the `KubernetesTokenVerifier` class:

```java
import com.netcracker.cloud.security.core.utils.k8s.KubernetesTokenVerificationException;
import com.netcracker.cloud.security.core.utils.k8s.KubernetesTokenVerifier;

public class ExampleAuthMiddleware {
    private KubernetesTokenVerifier verifier;

    public ExampleAuthMiddleware() {
        // pass the audience you want the tokens to contain
        this.verifier = new KubernetesTokenVerifier("audience");
    }

    public void authenticate(String token) throws KubernetesTokenVerificationException {
        verifier.verify(token);
    }
}
```

### Properties (Local Dev)

| property                                                     | value example | default value | description                                                 |
|--------------------------------------------------------------|---------------|---------------|-------------------------------------------------------------|
| com.netcracker.cloud.security.kubernetes.jwks.valid-interval | 12h           | 1d            | change valid duration of Json Web Keys from Kubernetes OIDC |
