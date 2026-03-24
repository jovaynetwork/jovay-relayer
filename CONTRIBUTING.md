# Contribution Guidelines

## Branch Strategy

We follow [Trunk-based development](https://trunkbaseddevelopment.com/) with `main` as the trunk branch.

### Branch Naming Convention

| Prefix | Purpose | Example |
|--------|---------|---------|
| `feat/` | New features and enhancements | `feat/task-execution-time-gap-control` |
| `fix/` | Bug fixes and hotfixes | `fix/blob-tx-hash-incorrect` |
| `refactor/` | Code refactoring without behavior change | `refactor/rollup-aggregator` |
| `conf/` | Configuration changes | `conf/expose-alarm-config` |
| `ci/` | CI/CD pipeline changes | `ci/custom-docker-tag` |
| `build/` | Build system changes | `build/jdk-21-support` |
| `deps/` | Dependency updates | `deps/hutool` |
| `docs/` | Documentation updates | `docs/add-project-documentation` |
| `test/` | Test additions or improvements | `test/more-negative-cases` |
| `perf/` | Performance improvements | `perf/get-trace-parallel-when-polling-blocks` |
| `style/` | Code style and formatting | `style/comments-for-services` |
| `release-` | Release branches | `release-0.12` |

### Commit Message Format

```
gitmoji: [type][module] Summary in English
```

- **gitmoji**: An emoji representing the commit type (see [gitmoji.dev](https://gitmoji.dev))
- **type**: `feat`, `fix`, `refactor`, `chore`, `test`, `docs`, `ci`, etc.
- **module**: `relayer`, `cli`, `commons`, `dal`, `sign-service`, `all`, etc.

Examples:
```
✨: [feat][relayer] Add task execution time gap control for scheduled duty tasks
🐛: [fix][relayer] Fix DA data deserialization from blobs
♻️: [refactor][relayer] Separate L1 TX_PACKAGED and TX_PENDING state processing
✅: [test][cli] Add unit tests for query rollup last committed batch
📄: [chore][relayer] Add Apache 2.0 license headers to all source files
```

## Contributing

- Fork the repository or create branches from `main`.
- Name your branch following the naming convention above.
- Make a pull request to the `main` branch of this repository.
- Wait for review and PR merged.

---

**Working on your first Pull Request?** You can learn how from this *free* series [How to Contribute to an Open Source Project on GitHub](https://kcd.im/pull-request)