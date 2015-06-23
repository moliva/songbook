(defproject songbook "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :source-paths ["src/clj" "src/cljs"]

  :dependencies [; Clojure + ClojureScript
                 [org.clojure/clojure "1.7.0-RC1"]
                 [org.clojure/core.match "0.3.0-alpha4"]
                 [org.clojure/clojurescript "0.0-3308" :scope "provided"]

                 ; React + Reagent
                 [cljsjs/react "0.13.1-0"]
                 [reagent "0.5.0"]
                 [reagent-forms "0.5.0"]
                 [reagent-utils "0.1.4"]

                 [ring-server "0.4.0"]
                 [ring "1.3.2"]
                 [ring/ring-defaults "0.1.4"]
                 [prone "0.8.2"]
                 [hiccup "1.0.5"]
                 [compojure "1.3.4"]
                 [secretary "1.2.3"]

                 ; Settings management
                 [environ "1.0.0"]

                 ; App specific
                 [com.cemerick/url "0.1.1"]]

  :repositories { "sonatype-oss-public" "https://oss.sonatype.org/content/groups/public/" }

  :plugins [[lein-ring "0.9.6"]
            [lein-environ "1.0.0"]
            [lein-asset-minifier "0.2.3"]
            [lein-bower "0.5.1"]]

  :bower-dependencies [[bootstrap "3.3.5"]
                       [font-awesome "4.3.0"]]

  :ring {:handler songbook.handler/app
         :uberwar-name "songbook.war"}

  :min-lein-version "2.5.0"

  :uberjar-name "songbook.jar"

  :main songbook.server

  :clean-targets ^{:protect false} ["resources/public/js"]

  :minify-assets
  {:assets
    {"resources/public/css/site.min.css" "resources/public/css/site.css"
     "resources/public/facss/fa.css" "resources/public/vendor/font-awesome/css/font-awesome.css"
     "resources/public/js/vendor.min.js" ["resources/public/vendor/jquery/dist/jquery.js"
                                          "resources/public/vendor/bootstrap/dist/js/bootstrap.js"]
     "resources/public/fonts/" ["resources/public/vendor/font-awesome/fonts/*" "resources/public/vendor/bootsrap/dist/fonts/*"]
     "resources/public/bootstrapcss/" ["resources/public/vendor/bootstrap/dist/css/bootstrap.css" 
                                                    "resources/public/vendor/bootstrap/dist/css/bootstrap-theme.css"
                                                    "resources/public/vendor/bootstrap/dist/css/bootstrap-theme.css.map"
                                                    "resources/public/vendor/bootstrap/dist/css/bootstrap.css.map"]
     }}

  :cljsbuild {:builds {:app {:source-paths ["src/cljs"]
                             :compiler {:output-to     "resources/public/js/app.js"
                                        :output-dir    "resources/public/js/out"
                                        :asset-path   "js/out"
                                        :optimizations :none
                                        :pretty-print  true}}}}

  :profiles {:dev {:repl-options {:init-ns songbook.repl
                                  :nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}

                   :dependencies [[ring-mock "0.1.5"]
                                  [ring/ring-devel "1.3.2"]
                                  [weasel "0.6.0"]
                                  [leiningen-core "2.5.1"]
                                  [com.cemerick/piggieback "0.2.1"]
                                  [org.clojure/tools.nrepl "0.2.10"]
                                  [pjstadig/humane-test-output "0.7.0"]]

                   :source-paths ["env/dev/clj"]
                   :plugins [[lein-figwheel "0.2.8"]
                             [lein-cljsbuild "1.0.6"]]

                   :injections [(require 'pjstadig.humane-test-output)
                                (pjstadig.humane-test-output/activate!)]

                   :figwheel {:http-server-root "public"
                              :server-port 3449
                              :css-dirs ["resources/public/css"]
                              :ring-handler songbook.handler/app}

                   :env {:dev true}

                   :cljsbuild {:builds {:app {:source-paths ["env/dev/cljs"]
                                              :compiler {:main "songbook.dev"
                                                         :source-map true}}
}
}}

             :uberjar {:hooks [leiningen.cljsbuild minify-assets.plugin/hooks]
                       :env {:production true}
                       :aot :all
                       :omit-source true
                       :cljsbuild {:jar true
                                   :builds {:app
                                             {:source-paths ["env/prod/cljs"]
                                              :compiler
                                              {:optimizations :advanced
                                               :pretty-print false}}}}}})
