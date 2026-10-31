# Every day-to-day command of this project lives here. Run `make` to list them.
# The commands call Babashka tasks (bb.edn), so they work the same on any machine.

.DEFAULT_GOAL := help

# Arguments for `make run`, e.g. make run ARGS="app.log --match ERROR"
ARGS ?=

# Version declared in the source; releases are tagged with this value.
VERSION = $(shell bb -e "(require '[borba.mgrep.shared.constants :as c]) (print c/version)")

.PHONY: help run test test-clj lint fmt fmt-fix check clean release

help: ## List the available commands
	@awk 'BEGIN {FS = ":.*## "} /^[a-zA-Z_-]+:.*## / \
		{printf "  \033[36m%-16s\033[0m %s\n", $$1, $$2}' $(MAKEFILE_LIST)

run: ## Run mgrep from source: make run ARGS="app.log --match ERROR"
	@bb mgrep $(ARGS)

test: ## Run the tests with Babashka
	@bb test

test-clj: ## Run the tests with the Clojure CLI
	@clojure -X:test

lint: ## Lint sources, tests and scripts with clj-kondo
	@bb lint

fmt: ## Check formatting with cljfmt
	@bb fmt

fmt-fix: ## Fix formatting with cljfmt
	@bb fmt-fix

check: ## Run lint, format check and tests, in this order
	@bb check

clean: ## Remove build output
	@rm -rf target dist

release: check ## Tag the declared version on main and push the tag, which publishes it
	@test "$$(git branch --show-current)" = main \
		|| { echo "release must run from main"; exit 1; }
	@git diff --quiet && git diff --cached --quiet \
		|| { echo "working tree has uncommitted changes"; exit 1; }
	@grep -q "^## $(VERSION)$$" CHANGELOG.md \
		|| { echo "CHANGELOG.md has no '## $(VERSION)' section"; exit 1; }
	@git tag "$(VERSION)" && git push origin "$(VERSION)"
	@echo "Pushed tag $(VERSION); the Release workflow publishes it"
