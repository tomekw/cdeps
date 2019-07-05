# cdeps
Clojure + deps.edn, a basic guide.

After a rather long break from programming and Clojure I decided give them another go. When it comes to managing Clojure
projects, [Leiningen](https://leiningen.org/) is de-facto standard tool. Recently,
[Clojure CLI tools](https://clojure.org/guides/deps_and_cli) are becoming more and more popular, though. Switching to 
yet-another-build-tool doesn't have any pragmatic value, but it's perfect for learning purposes.

From a build tool I expect it to perform certain tasks:
1. Creating a project.
1. Managing source and tests paths.
1. Managing dependencies.
1. Running tests.
1. Building a self-contained JAR, a.k.a. uberjar.
1. Managing outdated dependencies.

Let's see how it's performed using Clojure CLI tools, a.k.a. deps.edn.

## Creating a project
Leiningen allows to generate a project structure simply by invoking:
```bash
$ lein new [template] [project-name]
```
We get a lot for free, but is it really needed? How is it done with Clojure CLI tools?

Imagine a simple project. It allows add and divide numbers, it also prints some example calculations when invoked.
We can start by simply creating a new directory:
```bash
$ mkdir cdeps && cd cdeps
```
Now, let's add an empty `deps.edn` file:
```clojure
;; /deps.edn
{}
```
And now we can start adding some actual code to the project.

## Managing source and tests paths
To demonstrate the feature of managing source paths we will put our code at `src/main/clojure`.
```bash
$ mkdir -p src/main/clojure
```
`deps.edn` is no magic so we can just set the path in the file:
```clojure
;; deps.edn
{:paths ["src/main/clojure"]}
```
Now, we can write the calculator code:
```clojure
;; src/main/clojure/com/tomekw/cdeps/calculator.clj
(ns com.tomekw.cdeps.calculator)

(defn plus [a b]
  (+ a b))

(defn divide [a b]
  (/ a b))
```

## Managing dependencies
In such a simple project there is no real need to add external dependencies. We can always specify the Clojure version
we would like to use, though:
```clojure
;; deps.edn
{:paths ["src/main/clojure"]
 :deps  {org.clojure/clojure {:mvn/version "1.10.1"}}}
```
Clojure CLI tools allow to specify local and git dependencies too, see
[documentation and more examples](https://clojure.org/guides/deps_and_cli#_using_local_libraries).

## Running tests
The calculator we wrote is super simple but we can still write some tests:
```clojure
;; test/main/clojure/com/tomekw/cdeps/calculator_test.clj
(ns com.tomekw.cdeps.calculator-test
  (:require [clojure.test :refer :all]
            [com.tomekw.cdeps.calculator :refer :all]))

(deftest adding-numbers
  (is (= 4 (plus 2 2))))

(deftest dividing-numbers
  (is (= 2 (divide 4 2))))

(deftest dividing-numbers-by-zero
  (is (thrown? ArithmeticException (divide 1 0))))
```
Now we need to run them to make sure they pass. We have to add an alias (a command we will run), and a test runner,
as an extra dependency. I picked [kaocha](https://github.com/lambdaisland/kaocha). Also, we need to tell the runner
where the tests are located:
```clojure
;; deps.edn
{:paths   ["src/main/clojure"]
 :deps    {org.clojure/clojure {:mvn/version "1.10.1"}}
 :aliases {:test {:extra-paths ["test/main/clojure"]
                  :extra-deps  {lambdaisland/kaocha {:mvn/version "0.0-529"}}
                  :main-opts   ["-m" "kaocha.runner"]}}}
```
Here is the test report:
```bash
$ clj -Atest
[(...)]
3 tests, 3 assertions, 0 failures.
```

## Building a self-contained JAT, a.k.a. uberjar
Presume, we would like to print example calculations to the console. Let's add the code to do that:
```clojure
;; src/main/clojure/com/tomekw/cdeps/core.clj
(ns com.tomekw.cdeps.core
  (:gen-class)
  (:require [com.tomekw.cdeps.calculator :refer :all]))

(defn -main [& args]
  (do (println (format "2 + 2 is %s" (plus 2 2)))
      (println (format "4 / 2 is %s" (divide 4 2)))))
```
To run the main function we can invoke the following command:
```bash
$ clj -m com.tomekw.cdeps.core
2 + 2 is 4
4 / 2 is 2
```
It could be burdensome for the users of our calculator to install Clojure. To avoid this, we can package our project
as a standalone Java JAR. There is number of tools to do that, like [cambada](https://github.com/luchiniatwork/cambada),
but I've decided to try out [uberdeps](https://github.com/tonsky/uberdeps). Let's add a proper configuration first:
```clojure
;; deps.edn
{:paths   ["src/main/clojure"]
 :deps    {org.clojure/clojure {:mvn/version "1.10.1"}}
 :aliases {:test    {:extra-paths ["test/main/clojure"]
                     :extra-deps  {lambdaisland/kaocha {:mvn/version "0.0-529"}}
                     :main-opts   ["-m" "kaocha.runner"]}
           :uberjar {:extra-deps {uberdeps {:mvn/version "0.1.4"}}
                     :main-opts  ["-m" "uberdeps.uberjar" "--target" "target/cdeps-0.1.0.jar"]}}}
```
To package the project we simply run:
```bash
$ clj -Auberjar
[uberdeps] Packaging target/cdeps-0.1.0.jar...
+ src/main/clojure/**
+ org.clojure/clojure 1.10.1
.   org.clojure/core.specs.alpha 0.2.44
.   org.clojure/spec.alpha 0.2.176
[uberdeps] Packaged target/cdeps-0.1.0.jar in 567 ms
```
And now we can run the project with Java:
```bash
$ java -cp target/cdeps-0.1.0.jar clojure.main -m com.tomekw.cdeps.core
2 + 2 is 4
4 / 2 is 2
```

## Managing outdated dependencies
It's often needed to manage the versions of all dependencies we put into our `deps.edn` file. There is a tool named
[depot](https://github.com/Olical/depot):
```clojure
;; deps.edn
{:paths   ["src/main/clojure"]
 :deps    {org.clojure/clojure {:mvn/version "1.10.1"}}
 :aliases {:test     {:extra-paths ["test/main/clojure"]
                      :extra-deps  {lambdaisland/kaocha {:mvn/version "0.0-529"}}
                      :main-opts   ["-m" "kaocha.runner"]}
           :outdated {:extra-deps {olical/depot {:mvn/version "1.8.4"}}
                      :main-opts  ["-m" "depot.outdated.main" "-a" "outdated"]}
           :uberjar  {:extra-deps {uberdeps {:mvn/version "0.1.4"}}
                      :main-opts  ["-m" "uberdeps.uberjar" "--target" "target/cdeps-0.1.0.jar"]}}}
```
Everything should be up to date:
```bash
$ clj -Aoutdated
All up to date!
```

## Summary
This guide covers basic use-cases in the daily workflow with Clojure. Of course there is alwats more than I presented
here, like deploying the project to [Clojars](https://clojars.org). The process is still not fully automated and I will
try to cover it with the next post.
