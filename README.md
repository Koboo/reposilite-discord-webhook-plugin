# Reposilite Discord WebHook Plugin

This plugin allows posting of artifact deployments on Reposilite via Discord webhooks.

## RegEx Templates

- Post every file which ends with ``.jar``
````regexp
.*(\.jar$)
````

- Post every file which ends with ``{number}.jar``
````regexp
.*([0-9]\.jar$)
````

- Post every file which ends with
  - ``{number}.jar``
  - ``{number}-all.jar`` (shaded/shadow)
  - ``{number}-sources.jar``
  - ``{number}-javadoc.jar``
````regexp
.*([0-9]\.jar|[0-9]-all\.jar|[0-9]-sources\.jar|[0-9]-javadoc\.jar$)
````

## Screenshots

#### Root Settings

![Settings-1](https://i.imgur.com/roRI16V.png)

#### Repository Settings

![Settings-2](https://i.imgur.com/Gai4BS4.png)

#### Discord Post

![Discord](https://i.imgur.com/pETPrdU.png)