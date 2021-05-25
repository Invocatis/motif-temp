(ns motif.runner
  (:require
   [doo.runner :refer-macros [doo-tests]]
   [motif.core-test]))

(doo-tests 'motif.core-test)
