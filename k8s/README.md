# Core Utils - TLS Utilities

A lightweight Java library providing support for providing and verifying Kubernetes projected volume tokens.

## Features

- Lightweight API to get Kubernetes projected volume tokens by audience
- Cache tokens and only re-read from file system only when they are changed
- Customizable via SPI
- Make OIDC discovery to Kubernetes OIDC provider and verify projected volume tokens from Kubernetes
- Cache verification keys and refetch when KID from the token is not found in the cache

## Requirements

- Java 17+
- Maven 3.x

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

#### Usage of `getToken` function
getToken is used for retrieving a Kubernetes projected volume token by audience.
Only use predefined constants for audience argument. They are declared in the `K8sTokenAudiences` class.
For example to get a token with audience `K8sTokenAudiences.NETRACKER` do the following:
```java
import com.netcracker.cloud.security.core.utils.k8s.KubernetesTokenSource;

public class Service {
	public Data getData() {
		String token = KubernetesTokenSource.getToken(K8sTokenAudiences.NETRACKER);
		return doRequestWithToken(token);
	}
}
```

#### Audiences
`K8sTokenAudiences` class has defined audiences for tokens.

|Audience|Usage|
|---|---|
|`K8sTokenAudiences.NETCRACKER`|Requests to microservices withing the same namespace|
|`K8sTokenAudiences.DBAAS`|Requests to Dbaas infra service|
|`K8sTokenAudiences.MAAS`|Requests to Maas infra service|
|`K8sTokenAudiences.DEFAULT`|Requests to Kubernetes API|

#### Configuration
- Change the directory where tokens are located
```text
com.netcracker.cloud.security.kubernetes.tokens.dir=/path/to/directory
```
- Change the directory where default token is located
```text
com.netcracker.cloud.security.kubernetes.tokens.dir=/path/to/directory
```
