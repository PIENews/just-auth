(ns just-auth.test.core
  (:require [midje.sweet :refer :all]
            [just-auth
             [core  :as auth-lib]
             [schema :as schema]
             [messaging :as m]
             [util :as u]]
            [just-auth.db.account :as account]
            [clj-storage.core :as storage] 
            [schema.core :as s]
            [taoensso.timbre :as log]
            [buddy.hashers :as hashers]))

(fact "Create a new email based authentication record and validate schemas"
      (let [stores-m (storage/create-in-memory-stores ["account-store" "password-recovery-store"])
            hash-fns {:hash-fn hashers/derive
                      :hash-check-fn hashers/check}
            email-authentication (auth-lib/new-email-based-authentication stores-m
                                                 (m/new-stub-account-activator stores-m)
                                                 (m/new-stub-password-recoverer stores-m)
                                                 hash-fns)]

        (s/validate schema/AuthStores stores-m) => truthy

        (s/validate schema/HashFns hash-fns) => truthy

        (fact "Sign up a user and check that email has been sent"
              (let [email "some@mail.com"
                    uri "http://test.com"]
                (auth-lib/sign-up email-authentication
                                  "Some name"
                                  email
                                  "12345678"
                                  {:activation-uri uri}
                                  ["nickname"])
                (-> email-authentication :account-activator :emails deref count) => 1
                (let [account-created (account/fetch (:account-store stores-m) email)]
                  account-created => truthy
                  (:activated (account/fetch (:account-store stores-m) email)) => false
                  (fact "Activate account"
                        (let [activation-token (:activattion-token account-created)]
                          (auth-lib/activate-account email-authentication email
                                                     {:activation-link (u/construct-link {:uri uri
                                                                                         :token activation-token})}) => truthy
                          (:activated (account/fetch (:account-store stores-m) email)) => true)))))))
