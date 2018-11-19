# FDNS Business Rules Microservice

Foundation Services (FDNS) Business Rules Microservice is the service used to ingest and validate JSON files.

## Getting Started

To get started with the FDNS Business Rules Microservice you can use either `docker stack deploy` or `docker-compose`:

```yaml
version: '3.1'
services:
  fluentd:
    image: fluent/fluentd
    ports:
      - "24224:24224"
  fdns-ms-rules:
    image: cdcgov/fdns-ms-rules
    ports:
      - "8083:8083"
    depends_on:
      - fluentd
    environment:
      RULES_PORT: 8083
      RULES_FLUENTD_HOST: fluentd
      RULES_FLUENTD_PORT: 24224
```

[![Try in PWD](https://raw.githubusercontent.com/play-with-docker/stacks/master/assets/images/button.png)](http://play-with-docker.com?stack=https://raw.githubusercontent.com/CDCgov/fdns-ms-rules/master/stack.yml)

## Source Code

Please see [https://github.com/CDCgov/fdns-ms-rules](https://github.com/CDCgov/fdns-ms-rules) for the fdns-ms-rules source repository.

## Public Domain

This repository constitutes a work of the United States Government and is not subject to domestic copyright protection under 17 USC § 105. This repository is in the public domain within the United States, and copyright and related rights in the work worldwide are waived through the [CC0 1.0 Universal public domain dedication](https://creativecommons.org/publicdomain/zero/1.0/). All contributions to this repository will be released under the CC0 dedication.

## License

The repository utilizes code licensed under the terms of the Apache Software License and therefore is licensed under ASL v2 or later.

The container image in this repository is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the Apache Software License for more details.

## Privacy

This repository contains only non-sensitive, publicly available data and information. All material and community participation is covered by the Surveillance Platform [Disclaimer](https://github.com/CDCgov/template/blob/master/DISCLAIMER.md) and [Code of Conduct](https://github.com/CDCgov/template/blob/master/code-of-conduct.md).
For more information about CDC's privacy policy, please visit [http://www.cdc.gov/privacy.html](http://www.cdc.gov/privacy.html).

## Records

This repository is not a source of government records, but is a copy to increase collaboration and collaborative potential. All government records will be published through the [CDC web site](http://www.cdc.gov).