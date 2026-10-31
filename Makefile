# Every day-to-day command of this project lives here. Run `make` to list them.
# The commands call Babashka tasks (bb.edn), so they work the same on any machine.

.DEFAULT_GOAL := help

# Arguments for `make run`, e.g. make run ARGS="app.log --match ERROR"
ARGS ?=

# Tag checked by `make verify-version`, e.g. make verify-version TAG=v1
TAG ?=

# Version declared in the source; releases are tagged with this value.
VERSION = $(shell bb -e "(require '[borba.mgrep.shared.constants :as c]) (print c/version)")

.PHONY: help run test test-clj lint fmt fmt-fix check build smoke package \
        clean verify-version release

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

build: ## Build the native executable (needs GraalVM native-image)
	@bb build

smoke: ## Exercise the built executable
	@bb smoke

package: ## Archive the built executable with its install scripts
	@bb package

clean: ## Remove build output
	@rm -rf target dist

verify-version: ## Fail unless TAG equals the declared version: make verify-version TAG=v1
	@bb verify-version $(TAG)

release: check ## Tag the declared version on main and push the tag, which publishes it
	@test "$$(git branch --show-current)" = main \
		|| { echo "release must run from main"; exit 1; }
	@git diff --quiet && git diff --cached --quiet \
		|| { echo "working tree has uncommitted changes"; exit 1; }
	@grep -q "^## $(VERSION)$$" CHANGELOG.md \
		|| { echo "CHANGELOG.md has no '## $(VERSION)' section"; exit 1; }
	@git tag "$(VERSION)" && git push origin "$(VERSION)"
	@echo "Pushed tag $(VERSION); the Release workflow publishes it"
