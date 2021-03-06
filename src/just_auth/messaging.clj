;; Just auth - a simple two factor authenticatiokn library

;; part of Decentralized Citizen Engagement Technologies (D-CENT)
;; R&D funded by the European Commission (FP7/CAPS 610349)

;; Copyright (C) 2017 Dyne.org foundation

;; Sourcecode designed, written and maintained by
;; Aspasia Beneti  <aspra@dyne.org>

;; This program is free software: you can redistribute it and/or modify
;; it under the terms of the GNU Affero General Public License as published by
;; the Free Software Foundation, either version 3 of the License, or
;; (at your option) any later version.

;; This program is distributed in the hope that it will be useful,
;; but WITHOUT ANY WARRANTY; without even the implied warranty of
;; MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
;; GNU Affero General Public License for more details.

;; You should have received a copy of the GNU Affero General Public License
;; along with this program.  If not, see <http://www.gnu.org/licenses/>.

(ns just-auth.messaging
  (:require [postal.core :as postal]
            [just-auth.db
             [account :as account] 
             [password-recovery :as password-recovery]]
            [taoensso.timbre :as log]
            [auxiliary.translation :as t]
            [schema.core :as s]
            [just-auth.schema :refer [EmailConfig StoreSchema]]))

(defn postal-basic-conf [conf]
  {:host (:email-server conf)
   :user (:email-user conf)
   :pass (:email-pass conf)
   :ssl true})

;; TODO: does it make sense to split this into two protocols? One for messaging and one for d updates?
(defprotocol Email
  "A generic function that sends an email and updates db fields"
  (email-and-update! [this email link]))

(def EmailMessagingSchema just_auth.messaging.Email)

(defn- send-email [conf email subject body]
  (assert (:email-address conf) "No :email-address found in the mail config")
  (postal/send-message 
   (postal-basic-conf conf)
   {:from (:email-address conf)
    :to [email]
    :subject subject
    :body body}))

(defrecord AccountActivator [email-conf account-store]
  Email
  (email-and-update! [_ email activation-link]
    (let [email-response (if (account/update-activation-link! account-store email activation-link)
                           (if-let [admin-email (:email-admin email-conf)]
                             (send-email email-conf admin-email (t/locale [:email :account-activator :title]) (str (t/locale [:email :account-activator :content]) activation-link))
                             (send-email email-conf email (t/locale [:email :account-activator :title]) (str (t/locale [:email :account-activator :content]) activation-link)))
                           false)]
      (if (= :SUCCESS (:error email-response))
        email-response
        false))))

(s/defn ^:always-validate new-account-activator
  [email-conf :- EmailConfig
   account-store :- StoreSchema]
  (map->AccountActivator {:email-conf email-conf
                          :account-store account-store}))

(defrecord PasswordRecoverer [email-conf password-recovery-store]
  Email
  (email-and-update! [_ email password-recovery-link]
    (let [email-response       (if (password-recovery/new-entry! password-recovery-store email password-recovery-link)
                                 (send-email email-conf email
                                             (t/locale [:email :password-recoverer :title])
                                             (str (t/locale [:email :password-recoverer :content1]) email (t/locale [:email :password-recoverer :content2])  password-recovery-link (t/locale [:email :password-recoverer :content3])))
                                 false)]
      (if (= :SUCCESS (:error email-response))
        email-response
        false))))

(defn- update-emails [emails link-map email]
  (swap! emails conj
         (merge link-map {:email email
                          ;; the SUCCESS is needed to imitate poster responses
                          :error :SUCCESS})))

(s/defn ^:always-validate new-password-recoverer
  [email-conf :- EmailConfig
   password-recovery-store :- StoreSchema]
  (map->PasswordRecoverer {:email-conf email-conf
                           :password-recovery-store password-recovery-store}))

(defrecord StubAccountActivator [emails email-config account-store]
  Email
  (email-and-update! [_ email activation-link]
    (if-let [admin-email (:email-admin email-config)]
      (update-emails emails {:activation-link activation-link} admin-email)
      (update-emails emails {:activation-link activation-link} email)) 
    (account/update-activation-link! account-store email activation-link) 
    (first @emails)))

(defn new-stub-account-activator
  ([stores email-config]
   (new-stub-account-activator stores email-config (atom [])))
  ([stores email-config emails]
   (->StubAccountActivator emails email-config (get stores "account"))))

(defrecord StubPasswordRecoverer [emails password-recovery-store]
  Email
  (email-and-update! [_ email password-recovery-link]
    (update-emails emails {:password-recovery-link password-recovery-link} email)
    (password-recovery/new-entry! password-recovery-store
                                  email password-recovery-link)
    (first @emails)))

(defn new-stub-password-recoverer
  ([stores]
   (new-stub-password-recoverer (get stores "passwordrecovery") (atom [])))
  ([stores emails]
   (->StubPasswordRecoverer emails (get stores "passwordrecovery"))))
