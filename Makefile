.PHONY: test run bend-check check

test:
	clojure -M:test

run:
	clojure -M:run

bend-check:
	bend bend/main.bend

check: test bend-check

