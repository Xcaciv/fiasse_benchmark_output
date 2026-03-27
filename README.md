# fiasse_benchmark_output

Generated code produced by the [FIASSE Benchmark](https://github.com/Xcaciv/fiasse_benchmark) automation scripts for side-by-side comparison of AI coding tools and security plugin configurations.

**NOTE:** This repository is intended for benchmarking and comparison purposes only. The generated code may contain security flaws or other issues and should NOT be used in production without thorough review and remediation.

**NOTE:** The results in this repository may be incomplete for any number of reasons, it is a work in progress.

## Purpose

This repository contains the output of automated PRD-to-code generation runs. Each run feeds the same PRD specification to an AI coding CLI and captures the generated project, enabling direct comparison across tools, languages, and security configurations.

## Repository Structure

```
<root>/
├── claude/          ← Claude Code CLI
├── copilot/         ← GitHub Copilot CLI
└── opencode/        ← OpenCode CLI
```

### Level 1 — CLI Coding Client

The top-level folders separate output by the AI coding tool used to generate the code:

| Folder | Tool |
|--------|------|
| `claude/` | [Claude Code](https://docs.anthropic.com/en/docs/claude-code) (`claude --print`) |
| `copilot/` | [GitHub Copilot CLI](https://docs.github.com/en/copilot/reference/copilot-cli-reference) (`copilot agent run`) |
| `opencode/` | [OpenCode CLI](https://github.com/opencode-ai/opencode) |

### Level 2 — Target Language / Platform

Under each CLI folder are subfolders for each target language:

| Folder | Platform |
|--------|----------|
| `aspnet/` | ASP.NET Core (C#) |
| `jsp/` | Java JSP / Servlets |
| `node/` | Node.js / Express |

### Level 3 — Run Style

Each language folder contains one or more of these run-style folders:

| Folder | Description |
|--------|-------------|
| `rawdog/` | **Unassisted generation** — plain PRD prompt with no security plugin active |
| `securable/` | **Securable plugin run** — generation with the [FIASSE/SSEM security plugin](https://github.com/Xcaciv/securable-claude-plugin) applied, constraining the AI to follow secure engineering practices |
| `fiassed/` | **FIASSE-enhanced PRD run** — the PRD is first enriched with [FIASSE](https://github.com/Xcaciv/securable_software_engineering/blob/main/docs/FIASSE-RFC.md) and [ASVS](https://owasp.org/www-project-application-security-verification-standard/) requirements before generation |

### Full Example

```
fiasse_benchmark_output/
├── claude/
│   ├── aspnet/
│   │   ├── rawdog/        ← ASP.NET, Claude Code, no plugin
│   │   ├── securable/     ← ASP.NET, Claude Code, Securable plugin
│   │   └── fiassed/       ← ASP.NET, Claude Code, FIASSE-enhanced PRD
│   ├── jsp/
│   │   ├── rawdog/
│   │   ├── securable/
│   │   └── fiassed/
│   └── node/
│       ├── rawdog/
│       ├── securable/
│       └── fiassed/
├── copilot/
│   ├── aspnet/
│   │   ├── rawdog/
│   │   ├── securable/
│   │   └── fiassed/
│   ├── jsp/
│   │   ├── rawdog/
│   │   ├── securable/
│   │   └── fiassed/
│   └── node/
│       ├── rawdog/
│       ├── securable/
│   │   └── fiassed/
└── opencode/
    ├── aspnet/
    │   ├── rawdog/
    │   ├── securable/
│   │   └── fiassed/
    ├── jsp/
    │   ├── rawdog/
    │   ├── securable/
│   │   └── fiassed/
    └── node/
        ├── rawdog/
        ├── securable/
│   │   └── fiassed/
```

## Generation Scripts

All code in this repository was generated using the [FIASSE Benchmark PowerShell scripts](https://github.com/Xcaciv/fiasse_benchmark/tree/main/scripts/PowerShell). These scripts automate the process of feeding a PRD to each AI coding CLI across all language/mode combinations, producing the uniform folder structure shown above.

The three main scripts are:

| Script | Tool | Plugin |
|--------|------|--------|
| `run-codegen-claude.ps1` | Claude Code CLI | [securable-claude-plugin](https://github.com/Xcaciv/securable-claude-plugin) |
| `run-codegen-opencode.ps1` | OpenCode CLI | [securable-copilot](https://github.com/Xcaciv/securable-opencode-module) |
| `run-codegen-copilot-claude-plugin.ps1` | GitHub Copilot CLI | [securable-claude-plugin](https://github.com/Xcaciv/securable-claude-plugin) |
| `run-codegen-copilot.ps1` | **GitHub Copilot CLI | [securable-copilot](https://github.com/Xcaciv/securable-copilot) |

** this result is not currently included in the output

## Comparing Results

The uniform folder layout makes it straightforward to diff outputs:

| Comparison | What to diff | What it isolates |
|---|---|---|
| Tool vs tool (same plugin) | `claude/<lang>/securable/` vs `copilot/<lang>/securable/` | AI tool differences |
| Plugin vs no plugin | `rawdog/` vs `securable/` within any tool | Security plugin impact |
| Enhanced PRD vs baseline | `rawdog/` vs `fiassed/` within any tool | FIASSE/ASVS requirement enrichment impact |

## References

- [FIASSE Benchmark Scripts](https://github.com/Xcaciv/fiasse_benchmark/tree/main/scripts/PowerShell)
- [FIASSE RFC](https://github.com/Xcaciv/securable_software_engineering/blob/main/docs/FIASSE-RFC.md) — Framework for Integrating Application Security into Software Engineering
- [securable-claude-plugin](https://github.com/Xcaciv/securable-claude-plugin) — FIASSE/SSEM plugin for Claude Code
- [securable-copilot](https://github.com/Xcaciv/securable-copilot) — FIASSE/SSEM plugin for GitHub Copilot
- [OWASP ASVS](https://owasp.org/www-project-application-security-verification-standard/) — Application Security Verification Standard
