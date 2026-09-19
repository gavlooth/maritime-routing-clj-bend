.PHONY: install test run frontend-dev frontend-build bend-check bend-build bend-test check

install:
	npm install

test:
	clojure -M:test

run:
	clojure -M:run

frontend-dev:
	npm run dev

frontend-build:
	npm run build

bend-check:
	bend bend/physics.bend --checkup
	bend bend/routing.bend --checkup
	bend bend/main.bend >/dev/null

bend-build:
	mkdir -p build
	bend bend/main.bend -o build/maritime-bend

bend-test: bend-build
	test "$$($(CURDIR)/build/maritime-bend --threads 2)" = "304"

check: test bend-check bend-test frontend-build
