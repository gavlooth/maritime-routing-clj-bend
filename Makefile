.PHONY: test run bend-check bend-build bend-test check

test:
	clojure -M:test

run:
	clojure -M:run

bend-check:
	bend bend/physics.bend --checkup
	bend bend/routing.bend --checkup
	bend bend/main.bend >/dev/null

bend-build:
	mkdir -p build
	bend bend/main.bend -o build/maritime-bend

bend-test: bend-build
	test "$$($(CURDIR)/build/maritime-bend --threads 2)" = "304"

check: test bend-check bend-test
