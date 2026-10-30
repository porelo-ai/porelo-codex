# porelo-codex

<img src="doc/images/crazy-chicken.svg" width="350" alt="Crazy chicken - just hatched">

> **Crazy chicken - just hatched.** Interfaces, formats and even the purpose may change any day. Use it only to try ideas, never to build on.

Adapts [Codex CLI](https://github.com/openai/codex) for
[Porelo](https://github.com/porelo-ai/porelo): in a hardened container, reaching only what it needs,
with your credential kept on the machine and its work waiting for your review.
It is an adapter, not Porelo: it depends on Porelo's published agent API alone, never on Porelo's
implementation. Session, waiting and wake acceptance pass with an OpenAI API key, and a ChatGPT
sign-in is brokered and renewed on the host. See [the documentation](doc/index.md), [decisions](doc/decisions.md)
and [issues/](issues/README.md).

## More

- [CONTRIBUTING.md](CONTRIBUTING.md) - how to change it, and [SECURITY.md](SECURITY.md) - how to report a vulnerability.

## Licence

GNU General Public License v3.0 or later. See [LICENSE](LICENSE).
