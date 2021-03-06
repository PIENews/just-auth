;; Just auth - a simple two factor authenticatiokn library

;; part of Decentralized Citizen Engagement Technologies (D-CENT)
;; R&D funded by the European Commission (FP7/CAPS 610349)

;; Copyright (C) 2017-2020 Dyne.org foundation

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

(ns just-auth.db.account
  (:require [clj-storage.core :as storage]
            [clojure.string :as str]
            [taoensso.timbre :as log]))

;; TODO shcema
(defn- generate-hash [password hash-fn]
  (hash-fn password {:alg :pbkdf2+sha512}))

(defn new-account!
  [account-store
   {:keys [name email password flags othernames activated] :as account-map}
   hash-fn]
  (storage/store! account-store (log/spy (-> account-map
                                             (assoc :activated (or activated false))
                                             (assoc :flags (or flags ""))
                                             (update :password #(generate-hash % hash-fn))))))

(defn activate! [account-store email]
  (storage/update! account-store {:account/email email} {:account/activated true}))

(defn fetch [account-store email]
  (some-> (first (storage/query account-store {:account/email email} {}))
          (update :account/flags (fn [flags]
                                   (if (str/blank? flags)
                                     []
                                     (mapv #(keyword %) (str/split flags #" ")))))))

(defn fetch-by-activation-link [account-store activation-link]
  (first (storage/query account-store {:account/activationlink activation-link} {})))

(defn update-activation-link! [account-store email activation-link]
  (storage/update! account-store {:account/email email} {:account/activationlink activation-link}))

(defn delete! [account-store email]
  (storage/delete! account-store email))

;; TODO schema
(defn correct-password? [account-store email candidate-password hash-check-fn]
  (hash-check-fn candidate-password
   (:account/password (fetch account-store email))))

(defn update-password! [account-store email password hash-fn]
  (storage/update! account-store {:account/email email} {:account/password (generate-hash password hash-fn)}))

(defn add-flag! [account-store email flag]
  (storage/update! account-store {:ACCOUNT/EMAIL email} (str "FLAGS = FLAGS || '" flag " '")))

(defn remove-flag! [account-store email flag]
  (storage/update! account-store {:account/email email} (str "FLAGS = REPLACE(FLAGS,'" flag " ','')")))

(defn list-accounts [account-store params]
  (storage/query account-store params {}))
