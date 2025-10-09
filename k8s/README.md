# Core Utils - Kubernetes projected volume token utilities

A lightweight Java library providing support for working with Kubernetes projected volume tokens.

## Features

- Lightweight API to get Kubernetes projected volume tokens by audience
- Cache tokens and only re-read from file system only when they are changed
- Customizable via SPI
- Make OIDC discovery to Kubernetes OIDC provider and verify projected volume tokens from Kubernetes
- Cache verification keys and refetch when KID from the token is not found in the cache

## Installation

Add the dependency to your Maven project:

```xml
<dependency>
    <groupId>com.netcracker.cloud.security.core.utils</groupId>
    <artifactId>k8s-utils</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```
## Usage

### KubernetesTokenSource

#### How get a token?
`getToken` is used for retrieving a Kubernetes projected volume token by audience.
Only use predefined constants for audience argument. They are declared in the `KubernetesTokenAudiences` class.
For example to get a token with audience `KubernetesTokenAudiences.NETRACKER` do the following:
```java
import com.netcracker.cloud.security.core.utils.k8s.KubernetesTokenSource;

public class Service {
	public Data getData() {
		String token = KubernetesTokenSource.getToken(KubernetesTokenAudiences.NETRACKER);
		return doRequestWithToken(token);
	}
}
```

#### How get the default Kubernetes service account token?
If you want to make requests to Kubernetes API and need to get the default service account token use the `KubernetesDefaultToken` class:
```java
import com.netcracker.cloud.security.core.utils.k8s.KubernetesTokenSource;

public class Service {
	public Data getData() {
		String token = KubernetesDefaultToken.getToken();
		return doRequestToKubernetesApi(token);
	}
}
```

#### Audiences
`KubernetesTokenAudiences` class has defined audiences for tokens.

|Audience|Usage|
|---|---|
|`KubernetesTokenAudiences.NETCRACKER`|Requests to microservices withing the same namespace|
|`KubernetesTokenAudiences.DBAAS`|Requests to Dbaas infra service|
|`KubernetesTokenAudiences.MAAS`|Requests to Maas infra service|

#### Configuration
- Change the directory where tokens are located use property:
```text
com.netcracker.cloud.security.kubernetes.tokens.dir=/path/to/directory
```
- Change the directory where default Kubernetes service account is located use property:
```text
com.netcracker.cloud.security.kubernetes.serviceaccount.dir=/path/to/directory
```
- Change the interval of polling events from file system about token rotations use property `com.netcracker.cloud.security.kubernetes.tokens.polling.interval`. Value must be a valid java `Duration`
```text
com.netcracker.cloud.security.kubernetes.tokens.polling.interval=2m
```
- Provide your own implementation of `TokenSource`. For example if want to mock the behaviour of `KubernetesTokenSource` you can provide your own implementation.
In `resources/META_INF/services/com.netcracker.cloud.security.core.utils.k8s.TokenSource` file place the path to the class that implements `TokenSource`. For example for class:
```java
package path.to.package;

import com.netcracker.cloud.security.core.utils.k8s.Priority;

@Priority(10)
public class MockTokenSource {
	public String getToken(String audience) {
		return "test-token";
	}
}
```
Place following in `resources/META_INF/services/com.netcracker.cloud.security.core.utils.k8s.TokenSource`:
```text
path.to.package.MockTokenSourceImpl
```
- Provide your own `ScheduledExecutorService` for `WatchingTokenSource`. Extend `WatchingTokenSource` and override the `getScheduledExecutorService` method.

### KubernetesTokenVerifier

#### Verify tokens
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

#### Configuration
- Change valid duration of Json Web Keys from Kubernetes OIDC.
`KubernetesTokenVerifier` fetches key to validate tokens from Kubernetes OIDC. They are periodically refreshed and only valid for a certain duration. To override use the config property:
```text
com.netcracker.cloud.security.kubernetes.jwks.valid-interval=12h
```
