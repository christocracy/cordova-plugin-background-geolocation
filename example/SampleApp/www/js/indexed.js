/*jshint browser: true, onevar: false, strict: false, eqeqeq: false */
/*global IDBKeyRange, define, exports, module */
// index.js
// Provides easy interaction with indexedDB
// ---
// Part of the Riggr SPA framework <https://github.com/Fluidbyte/Riggr> and released
// under the MIT license. This notice must remain intact.
(function (root, factory) {
  if (typeof define === 'function' && define.amd) {
    define([], factory);
  } else if (typeof exports === 'object') {
    module.exports = factory();
  } else {
    root.indexed = factory();
  }
}(this, function () {

  function indexed(dbstore) {

    // Hackery
    // cannot monkey patch window.indexedDB in Safari (dammn you!)
    var _indexedDB = window.indexedDB || window.webkitIndexedDB || window.mozIndexedDB;

    return {

      // Ensure callback exists and is function, then do it...
      processCB: function (cb, out) {
        if (cb && typeof cb === 'function') {
          var err = (out === false) ? true : false;
          cb(err, out);
        } else {
          console.error('Improper callback');
        }
      },

      // Parse query to string for evaluation
      parseQuery: function (query) {
        var res = [];
        if (!Array.isArray(query)) {
          query = [query];
        }
        query.forEach(function (cond) {
          // Set key
          var key = Object.keys(cond);
          // Check for conditional
          if (typeof cond[key] === 'object') {
            var condition = Object.keys(cond[key]);
            res.push({
              field: key[0],
              operand: condition[0],
              value: cond[key][condition]
            });
          } else {
            // Direct (==) matching
            res.push({
              field: key[0],
              operand: '$eq',
              value: cond[key]
            });
          }
        });
        return res;
      },

      // Check data type
      checkType: function (obj) {
        return ({}).toString.call(obj).match(/\s([a-zA-Z]+)/)[1].toLowerCase();
      },

      // Create indexedDB store
      create: function (cb) {
        var self = this;
        var request = _indexedDB.open(dbstore);

        // Handle onupgradeneeded
        request.onupgradeneeded = function (e) {
          var db = e.target.result;

          // Create store
          db.createObjectStore(dbstore, {
            keyPath: '_id',
            autoIncrement: false
          });
        };

        request.onsuccess = function (e) {
          e.target.result.close();
          self.processCB(cb, true);
        };

        request.onerror = function () {
          self.processCB(cb, false);
        };
      },

      // Add item to the store
      insert: function (data, cb) {
        var self = this;
        var request = _indexedDB.open(dbstore);
        request.onsuccess = function (e) {
          // Setup trans and store
          var db = e.target.result;
          var trans = db.transaction([dbstore], self.IDBTransactionModes.READ_WRITE);
          var store = trans.objectStore(dbstore);
          var i, returnQuery;

          function putNext() {
            if (i < data.length) {
              // Set _id
              data[i]._id = new Date().getTime() + i;
              // Insert, call putNext recursively on success
              store.put(data[i]).onsuccess = putNext;
              //console.log('data', data[i]);
              i++;
            } else {
              // Complete
              self.find(returnQuery, cb);
            }
          }

          if (self.checkType(data) === 'array') {
            // Insert array of items
            i = 0;
            returnQuery = {
              '_id': {
                '$gte': new Date().getTime()
              }
            };

            putNext();

          } else {
            // Insert single item
            data._id = new Date().getTime();
            var request = store.put(data).onsuccess = function (e) {
              // Run select to return new record
              self.find({
                _id: data._id
              }, cb);
              db.close();
            };

            // Insert error
            request.onerror = function (e) {
              self.processCB(cb, false);
            };
          }
        };

        // General error
        request.onerror = function () {
          self.processCB(cb, false);
        };
      },

      // Traverse data
      traverse: function (query, data, cb) {
        var self = this;
        var request = _indexedDB.open(dbstore);

        request.onsuccess = function (e) {

          var db = e.target.result;
          var trans = db.transaction([dbstore], self.IDBTransactionModes.READ_WRITE);
          var store = trans.objectStore(dbstore);

          // Setup cursor request
          var keyRange = IDBKeyRange.lowerBound(0);
          var cursorRequest = store.openCursor(keyRange);
          var results = [];

          cursorRequest.onsuccess = function (e) {
            //jshint maxstatements: 24, maxcomplexity: 12
            var result = e.target.result;
            var prop;

            // Stop on no result
            if (!result) {
              return;
            }

            function evaluate(val1, op, val2) {
              switch (op) {
              case '$gt':
                return val1 > val2;
              case '$lt':
                return val1 < val2;
              case '$gte':
                return val1 >= val2;
              case '$lte':
                return val1 <= val2;
              case '$ne':
                return val1 != val2;
              case '$eq':
                return val1 == val2;
              case '$like':
                return new RegExp(val2, 'i').test(val1);
              }
            }
            // Test query
            if (query) {
              var match = true;
              query.forEach(function (cond) {
                match = match && evaluate(result.value[cond.field], cond.operand, cond.value);
              });
              // Evaluate test condition
              if (match) {
                // Check if update
                if (self.checkType(data) === 'object') {
                  for (prop in data) {
                    result.value[prop] = data[prop];
                  }
                  result.update(result.value);
                }
                // Check if delete
                if (data === 'delete') {
                  result.delete(result.value._id);
                }
                // Push to array
                results.push(result.value);
              }
            } else {
              // Check if update
              if (self.checkType(data) === 'object') {
                for (prop in data) {
                  result.value[prop] = data[prop];
                }
                result.update(result.value);
              }
              // Check if delete
              if (data === 'delete') {
                result.delete(result.value._id);
              }
              // Push to array
              results.push(result.value);
            }
            // Move on
            result.
            continue ();
          };

          // Entire transaction complete
          trans.oncomplete = function (e) {
            // Send results
            self.processCB(cb, results);
            db.close();
          };

          // Cursor error
          cursorRequest.onerror = function () {
            self.processCB(cb, false);
          };
        };

        // General error, cb false
        request.onerror = function () {
          self.processCB(cb, false);
        };

      },

      // Find record(s)
      find: function () {
        var query = false;
        var cb;
        // Check arguments to determine query
        if (arguments.length === 1 && typeof arguments[0] === 'function') {
          // Find all
          cb = arguments[0];
        } else {
          // Conditional find
          query = this.parseQuery(arguments[0]);
          cb = arguments[1];
        }
        this.traverse(query, false, cb);
      },

      // Update record(s)
      update: function () {
        var query = false;
        var data;
        var cb;
        // Check arguments to determine query
        if (arguments.length === 2 && typeof arguments[1] === 'function') {
          // Update all
          data = arguments[0];
          cb = arguments[1];
        } else {
          // Update on match
          query = this.parseQuery(arguments[0]);
          data = arguments[1];
          cb = arguments[2];
        }
        this.traverse(query, data, cb);
      },

      // Delete record(s)
      delete: function () {
        var query = false;
        var cb;
        // Check arguments to determine query
        if (arguments.length === 1 && typeof arguments[0] === 'function') {
          // Find all
          cb = arguments[0];
        } else {
          // Conditional find
          query = this.parseQuery(arguments[0]);
          cb = arguments[1];
        }
        this.traverse(query, 'delete', cb);
      },

      // Drop data store
      drop: function (cb) {
        var self = this;
        var deleteRequest = _indexedDB.deleteDatabase(dbstore);
        // Golden
        deleteRequest.onsuccess = function (e) {
          self.processCB(cb, true);
          self.create();
        };
        // Blocked
        deleteRequest.onblocked = function (e) {
          self.processCB(cb, false);
        };
        // Something worse
        deleteRequest.onerror = function () {
          self.processCB(cb, false);
        };
      },

      IDBTransactionModes: {
        'READ_ONLY': 'readonly',
        'READ_WRITE': 'readwrite',
        'VERSION_CHANGE': 'versionchange'
      }

    };

  }

  return indexed;

}));
