/*
 * Copied from https://github.com/monmohan/dsjslib
 * 
 * Converted to ES module, removed debugging stuff/logging statements and changed/added a few methods to kind of comply with JavaScript Map type
 * 
 */

export class BinarySearchTree {
    /**
     * @class BinarySearchTree
     * @classdesc Implementation of a Binary Search Tree Data structure
     * @param compareFn  {userCompareFn=}, fn(a,b) return 1 if b>a, -1 if b<a, 0 otherwise
     */
    constructor(compareFn) {
        this.root = null;
        this.size = 0;
        /**
         *
         * @param key
         * @param parent
         * @param leftChild
         * @param rightChild
         * @return {Object}
         */
        this.mkNode_ = function (key, val, parent, leftChild, rightChild) {
            return {key : key,
                parent : parent || null,
                leftChild : leftChild || null,
                rightChild : rightChild || null,
                height : 0,
                value : val,
                isLeftChild : function () {
                    return this.parent && this.parent.leftChild === this;
                },
                isRightChild : function () {
                    return this.parent && this.parent.rightChild === this;
                },
                inspect : function () {
                    return util && util.inspect({key : this.key, h : this.height,
                        L : this.leftChild, R : this.rightChild, p : (this.parent ? this.parent.key : null)}, {depth : null, colors : true});
                }

            };
        };

        this._compFn = function (node, key) {
            return compareFn ? compareFn.call(this, node.key, key)
                : (node.key < key ? 1 : node.key > key ? -1 : 0);

        };
        /**
         * Private method to find successor node
         * @param item
         * @param node
         * @return {*}
         */
        var successorNode = function (node) {
			if(!node) return null;
			if(node.rightChild)return minNode(node.rightChild);
				var n = node;			
				while (n && n.isRightChild()) {
					 n=n.parent;                  
				}
				return n.parent;
        };
        /**
         * Return the successor key value pair
         * @memberof BinarySearchTree.prototype
         * @instance
         * @function successor
         * @param item {*} key for which successor is needed
         * @returns {Object} {key:successor key,value: value}
         */
        this.successor = function (item) {
            var node = this.getKeyValue(item, this.root);
            var sc = successorNode(node);
            return sc && {key : sc.key, value : sc.value}

        };

        /**
         * Private method to return min node in tree
         * @param m
         * @return {*}
         */
        var minNode = function (m) {
            while (m.leftChild) {
                m = m.leftChild;
            }
            return m;
        };

        /**
         * Find min key
         * @memberof BinarySearchTree.prototype
         * @instance
         * @function min
         * @returns {Object} {key:minimum key,value: value}
         */
        this.min = function () {
			var mNode = this.root && minNode(this.root);
            return mNode && {key : mNode.key, value : mNode.value};

        };

        /**
         * Private return max node in tree
         * @param max
         * @return {*}
         */
        var maxNode = function (max) {
            while (max.rightChild) {
                max = max.rightChild;
            }
            return max;
        };

        /**
         * Find max key
         * @memberof BinarySearchTree.prototype
         * @instance
         * @function max
         * @returns {Object} {key:maximum key,value: value}
         */
        this.max = function () {
			var mNode = this.root && maxNode(this.root);
            return mNode && {key : mNode.key, value : mNode.value};
        };

        /**
         * Private method to find predecessor node
         * @param node
         * @return {*}
         */
        var predecessorNode = function (node) {
            if(!node) return null;
			if(node.leftChild)return maxNode(node.leftChild);
				var n = node;			
				while (n && n.isLeftChild()) {
					 n=n.parent;                  
				}
				return n.parent;
        };

        /**
         * Return the predecessor key value pair
         * @memberof BinarySearchTree.prototype
         * @instance
         * @function predecessor
         * @param key {*} key for which predecessor is needed
         * @returns {Object} {key:predecessor key,value: value}
         */
        this.predecessor = function (key) {
            var node = this.getKeyValue(key, this.root);
            var pNode = predecessorNode(node)
            return pNode && {key : pNode.key, value : pNode.value}
        };


    }

    /**
     * Insert a key value pair
     * @memberof BinarySearchTree.prototype
     * @instance
     * @param key
     * @param value
     * @returns {Object}
     */
    put(key, value) {
    	// Naive, what if key already exists! Also implement delete!
    	this.size++;

    	if (!this.root) {
            this.root = this.mkNode_(key, value);
            return this;
        }

        var cNode = this.root;
        var pNode = null;
        var isLeft = false;
        while (cNode) {
            pNode = cNode;
            if (this._compFn(cNode, key) == -1) {
                cNode = cNode.leftChild;
                isLeft = true;
            } else if (this._compFn(cNode, key) == 1) {
                cNode = cNode.rightChild;
                isLeft = false;
            } else {//replace
                cNode.value = value;
                break;
            }
        }
        //cNode should be null now
        var iNode = cNode;
        if (!cNode) {
            iNode = this.mkNode_(key, value, pNode);
            pNode[isLeft ? "leftChild" : "rightChild"] = iNode;
            this.reCalcHeight(iNode);
        }
        var tree = this;
        return {
            put : function (key, value) {
                return tree.put(key, value);
            },
            node : iNode
        };
    }
    
    set(key, value) {
    	return this.put(key, value);
    }

    reCalcHeight(pNode) {
        while (pNode) {
            pNode.height = Math.max((pNode.leftChild ? pNode.leftChild.height : -1),
                (pNode.rightChild ? pNode.rightChild.height : -1)) + 1;
            pNode = pNode.parent;
        }
    }

    /**
     * Inorder traversal, apply provided function on each  visited node
     * @memberOf BinarySearchTree.prototype
     * @instance
     * @param node {Object=} Start at root if not given
     * @param fn {function} Callback function called for every node visited
     */
    traverse(node, fn) {
        var args = Array.prototype.slice.call(arguments);
        if (args.length === 1) {
            if (Object.prototype.toString.call(args[0]) === '[object Function]') {
                node = this.root;
                fn = args[0];
            } else {
                fn = function (n) {
                    console.log(n.key);
                }
            }
        }

        if (!node)return;
        this.traverse(node.leftChild, fn);
        fn(node);
        this.traverse(node.rightChild, fn);
    }

    keysAsArray() {
    	var keys = [];
    	this.traverse((entry) => {
    		keys.push(entry.key);
    	});
    	return keys;
    }
    
    keys() {
    	return this.keysAsArray().values();
    }
    
    /**
     * Get value for a key
     * @memberOf BinarySearchTree.prototype
     * @instance
     * @param key
     * @returns {Object} {key:key used in query,value:value of the key }, null if key was not found
     */
    getKeyValue(key, node) {
        if (typeof key === 'undefined' || key === null)return null;
        var retKV = (typeof node === "undefined");
        if (retKV)node = this.root;
        var compFn = this._compFn;
        return recFind(key, node);
        function recFind(key, node) {
            if (!node) return null;
            if (compFn(node, key) === -1) return recFind(key, node.leftChild);
            if (compFn(node, key) === 1) return recFind(key, node.rightChild);
            if (compFn(node, key) === 0)return retKV ? {key : node.key, value : node.value} : node;
        }
    }
	
	has(key) {
		return this.getKeyValue(key) != null;
	}

    get(key) {
    	const kv = this.getKeyValue(key);
    	if (kv) {
    		return kv.value;
    	}
    	return null;
    }

    /**
     * Delete a key value pair from the Map.
     * @memberof BinarySearchTree.prototype
     * @instance
     * @function delete
     * @param  item {*} key to deleted
     */
    delete(item) {
        var node = this.getKeyValue(item, this.root),
            p,
            lc,
            child;
        if (node) {
            var num = node.leftChild ? (node.rightChild ? 2 : 1) : (node.rightChild ? 1 : 0);
            switch (num) {
                case 0:
                    p = node.parent;
                    if (p) {
                        lc = p.leftChild === node;
                        p[lc ? "leftChild" : "rightChild"] = null;
                        node = null;
                    }
                    break;
                case 1:
                    //single subtree
                    p = node.parent;
                    if (p) {
                        lc = p.leftChild === node;
                        child = node.leftChild || node.rightChild;
                        child.parent = p;
                        p[lc ? "leftChild" : "rightChild"] = child;
                        node = null;
                    } else {
                        //root
                        child = node.leftChild || node.rightChild;
                        lc = node.leftChild === child;
                        child.parent = null;
                    }
                    break;
                case 2:
                    var nextL = this.successor(node.key);
                    this['delete'](nextL.key);
                    node.key = nextL.key;
                    node.value = nextL.value;
            }
        }
    }

    checkInvariants(node) {
        if (typeof node === 'undefined') {
            node = this.root;
        }
        if (!node) return;
        var lc = node.leftChild, rc = node.rightChild;
        if (isDbg()) {
            console.log(util.format("lc=%s, rc=%s, node=%s",
                lc ? lc.key : "null", rc ? rc.key : "null", node.key))
        }
        var ok = (!lc || this._compFn(node, lc.key) === -1) &&
            (!rc || this._compFn(node, rc.key) === 1);

        if (!ok) {
            throw new Error("Invariant check failed at node " + node + " key=" + node.key);
        }
        this.checkInvariants(lc);
        this.checkInvariants(rc);
    }

    inspect() {
        return util.inspect(this.root, {depth : null, colors : true});
    }

    entrySet() {
        var entries = [];
        this.traverse(this.root, function (node) {
            entries.push({key : node.key, value : node.value});
        });
        return entries;
    }
}