# Issue tracker: OpenSpec

Work is tracked as OpenSpec changes under `openspec/`, managed with the `openspec` CLI. There is no GitHub-issue tracker; the OpenSpec change is the unit of work and the source of the "spec" axis of `code-review`.

## Conventions

- One change per directory: `openspec/changes/<change-slug>/`
- The spec is the change's artifacts (proposal + delta specs under `specs/` + tasks), per the schema chosen when the change was created
- Completed changes move to `openspec/changes/archive/`

## When a skill says "fetch the relevant ticket / spec"

Read the OpenSpec artifacts through the CLI, not by hand:

- List changes: `openspec list`
- Show a change's specs/status: `openspec show --change <name>`
- Working status / next artifact: `openspec status --change <name> --json`
- Next-artifact template: `openspec instructions <artifact> --change <name>`

The user normally passes a change name, branch, or commit. If a commit message references a change name, resolve it with `openspec show --change <name>`. If the path resolves, pass the specs to the review.

## When no spec exists

`code-review`'s Spec axis reports "no spec available". If a change name in the history does not resolve, ask the user where the spec lives.
