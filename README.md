# gumshoe

A debugging tool for tracking arguments passed to Clojure functions.

"Extremism in the defense of debugging is no vice!"

## Usage

In your project.clj:

```
[dato/gumshoe "0.1.5"]
```

Use `deft` in place of `defn` to have the arguments to the function defined in the namespace.

Arguments are prefixed with "-{name-of-function}-". For example:

```clojure
user> (deft add-ten [num]
        (+ num 10))
#'user/add-ten
user> (add-ten 2)
12
user> -add-ten-num
2
```

Works with destructuring:

```clojure
user> (deft destructure-example [{:keys [a b] :as args}]
        a)
#'user/destructure-example
user> -destructure-example-a
1
user> -destructure-example-b
nil
user> -destructure-example-args
{:a 1, :c 2}
```

Careful with recursion:
```clojure
user> (deft recur-example [x]
        (if (= 0 x)
          :done
          (recur (dec x))))
#'user/recur-example
user> (recur-example 10)
:done
user> -recur-example-x
0 ;; returns 0 because the value gets redefined on each trip through the function
```

## Automatically using deft in development

Gumshoe ships with a plugin that will replace clojure's `defn` with Gumshoe's `deft`. With the plugin, you'll automatically get tracking for every function in your project!

Either in your project's `project.clj` file or in the `:user` profile in `~/.lein/profiles.clj`

```
:plugins [[dato/gumshoe "0.1.5"]]
```

Putting gumshoe in your user profile is handy, but it's safer to create a separate profile just for gumshoe:

```
{:gumshoe {:plugins [[dato/gumshoe "0.1.5"]]}
 :user {:plugins ...}}
```

Then use with-profile in lein to use gumshoe only in dev:

```
lein with-profile +gumshoe run
```

## Testing and development

If you want to make changes and have a place to test them out, there is an
example service in the dev directory. To start it, run:

```
lein with-profile dev run -m gumshoe.dev
```

Then connect to the repl on port 3005 and open your browser to http://localhost:4579

The ports are configurable with `NREPL_PORT` and `HTTP_PORT`.

## TODO

1. Add motivation to the about section on the README
2. Turn on tracking for all functions in a namespace
3. Update trackers when functions are redefined, similar to ring's reload middleware

## License

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
