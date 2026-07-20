# Licensing and crawler protection

WhatIRead uses a **two-layer** approach: legal terms in the repository, and
technical crawler controls on the deployed site.

## How to adopt licenses (do not link from GitHub at runtime)

**Copy license text into this repository.** You do not submodule, npm-install, or
hot-link license files from GitHub. Recipients and courts need a **stable copy**
in your repo at each release/tag.

| File | Role |
|------|------|
| [LICENSE](../LICENSE) | Full **AGPL-3.0** text (copyleft — derivatives and network use must share source) |
| [NON-AI-LICENSE](./NON-AI-LICENSE) | **AI training prohibition**, adapted from [non-ai-licenses/non-ai-licenses](https://github.com/non-ai-licenses/non-ai-licenses) |
| [ADDITIONAL-TERMS.md](../ADDITIONAL-TERMS.md) | Plain-language summary and how the files combine |

### Why not switch entirely to non-ai-licenses?

The [non-ai-licenses](https://github.com/non-ai-licenses/non-ai-licenses) project
provides templates based on **permissive** licenses (MIT, Apache-2.0, BSD, …).
There is **no NON-AI-AGPL** template. WhatIRead keeps **AGPL-3.0 for copyleft**
and adds **NON-AI-LICENSE** as additional copyright conditions (see
[ADDITIONAL-TERMS.md](../ADDITIONAL-TERMS.md)).

### Process when setting up a new project (or updating)

1. Copy [NON-AI-MIT](https://raw.githubusercontent.com/non-ai-licenses/non-ai-licenses/main/NON-AI-MIT) (or another NON-AI-* template) and adapt the preamble for your base license — or use this repo’s [NON-AI-LICENSE](./NON-AI-LICENSE) as-is.
2. Replace `<copyright holders>` with your name or “Project Contributors”.
3. Keep the full **AGPL-3.0** text in `LICENSE` (unmodified body).
4. Point `README.md`, `pom.xml`, and package metadata to these files.
5. On GitHub: set the repository license to **AGPL-3.0** in Settings → General → License (GitHub has no “NON-AI” preset; document the extra terms in README).

### Updating NON-AI terms later

- Watch [non-ai-licenses](https://github.com/non-ai-licenses/non-ai-licenses) for template changes.
- If you change NON-AI-LICENSE, tag a new release so users know which terms apply.

---

## Crawler blocking (robots.txt + nginx)

| File | Role |
|------|------|
| [frontend/public/robots.txt](../frontend/public/robots.txt) | Advisory block list for AI crawlers ([ai.robots.txt](https://github.com/ai-robots-txt/ai.robots.txt)) |
| [frontend/nginx-block-ai-bots.conf](../frontend/nginx-block-ai-bots.conf) | **Enforced** 403 for those User-Agents at the edge |

`robots.txt` is **not** a license and is not legally binding; well-behaved bots honor it.
nginx returns **403** for listed AI bots even if they ignore robots.txt.

### Updating the crawler list

```bash
curl -sL -o frontend/public/robots.txt \
  https://raw.githubusercontent.com/ai-robots-txt/ai.robots.txt/main/robots.txt
# Re-apply the header comment at the top of robots.txt (see existing file).

curl -sL -o frontend/nginx-block-ai-bots.conf \
  https://raw.githubusercontent.com/ai-robots-txt/ai.robots.txt/main/nginx-block-ai-bots.conf
# Re-apply the header comment at the top of nginx-block-ai-bots.conf.
```

Subscribe to updates:  
https://github.com/ai-robots-txt/ai.robots.txt/releases.atom

### Bing / Microsoft training opt-out

Bing uses a separate opt-out (not covered by all AI user-agents). WhatIRead sets:

- `<meta name="robots" content="noarchive">` in `frontend/index.html`
- `X-Robots-Tag: noarchive` in nginx

See [ai.robots.txt Bing notes](https://github.com/ai-robots-txt/ai.robots.txt/blob/main/docs/additional-steps/bing.md).

---

## Not legal advice

Have a lawyer review before relying on these terms for commercial enforcement.
