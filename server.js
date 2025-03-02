const http = require('http');
const fs = require('fs');
const path = require('path');
const url = require('url');

const PORT = 3000;
const dbFilePath = path.join(__dirname, 'db.json');

// Helper: Read the JSON database
function readDB(callback) {
  fs.readFile(dbFilePath, 'utf8', (err, data) => {
    if (err) return callback(err);
    try {
      const db = JSON.parse(data);
      callback(null, db);
    } catch (parseError) {
      callback(parseError);
    }
  });
}

// Helper: Write to the JSON database
function writeDB(db, callback) {
  fs.writeFile(dbFilePath, JSON.stringify(db, null, 2), 'utf8', callback);
}

// Helper: Parse request body as JSON
function parseRequestBody(req, callback) {
  let body = '';
  req.on('data', chunk => {
    body += chunk.toString();
  });
  req.on('end', () => {
    try {
      const parsed = JSON.parse(body);
      callback(null, parsed);
    } catch (err) {
      callback(err);
    }
  });
}

// Helper: Validate transaction schema
function validateTransaction(tx) {
  if (typeof tx.userId !== 'number') return false;
  if (typeof tx.planId !== 'number') return false;
  if (!tx.planName || typeof tx.planName !== 'string') return false;
  if (!tx.transactionDate || isNaN(Date.parse(tx.transactionDate))) return false;
  if (!["Completed", "Cancelled"].includes(tx.status)) return false;
  if (typeof tx.amount !== 'number') return false;
  if (!tx.paymentMethod || typeof tx.paymentMethod !== 'string') return false;
  if (!tx.expiryDate || isNaN(Date.parse(tx.expiryDate))) return false;
  if (typeof tx.queueAfterActivePlan !== 'boolean') return false;
  return true;
}

// Create the HTTP server
const server = http.createServer((req, res) => {
  const parsedUrl = url.parse(req.url, true);
  const method = req.method;

  // --- Endpoints for Users ---
  if (parsedUrl.pathname.startsWith('/api/users') && method === 'GET') {
    readDB((err, db) => {
      if (err) {
        res.writeHead(500, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ error: 'Failed to read database' }));
        return;
      }
      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify(db.users));
    });
    return;
  }

  // GET /api/users/:id => return a specific user by ID
  if (parsedUrl.pathname.startsWith('/api/users/') && method === 'GET') {
    const userId = parseInt(parsedUrl.pathname.split('/').pop(), 10);
    readDB((err, db) => {
      if (err) {
        res.writeHead(500, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ error: 'Failed to read database' }));
        return;
      }
      const user = db.users.find(u => u.id === userId);
      if (!user) {
        res.writeHead(404, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ error: 'User not found' }));
        return;
      }
      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify(user));
    });
    return;
  }

  // PUT /api/users/:id => update a specific user by ID
  if (parsedUrl.pathname.startsWith('/api/users/') && method === 'PUT') {
    const userId = parseInt(parsedUrl.pathname.split('/').pop(), 10);
    parseRequestBody(req, (err, updatedData) => {
      if (err) {
        res.writeHead(400, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ error: 'Invalid JSON body' }));
        return;
      }
      readDB((err, db) => {
        if (err) {
          res.writeHead(500, { 'Content-Type': 'application/json' });
          res.end(JSON.stringify({ error: 'Failed to read database' }));
          return;
        }
        const userIndex = db.users.findIndex(u => u.id === userId);
        if (userIndex === -1) {
          res.writeHead(404, { 'Content-Type': 'application/json' });
          res.end(JSON.stringify({ error: 'User not found' }));
          return;
        }
        db.users[userIndex] = { ...db.users[userIndex], ...updatedData };
        writeDB(db, (err) => {
          if (err) {
            res.writeHead(500, { 'Content-Type': 'application/json' });
            res.end(JSON.stringify({ error: 'Failed to update user' }));
            return;
          }
          res.writeHead(200, { 'Content-Type': 'application/json' });
          res.end(JSON.stringify(db.users[userIndex]));
        });
      });
    });
    return;
  }

  // --- Endpoints for Transactions ---
  if (parsedUrl.pathname.startsWith('/api/transactions')) {
    const parts = parsedUrl.pathname.replace(/^\/+|\/+$/g, '').split('/');
    // GET /api/transactions => return all transactions
    if (method === 'GET' && parts.length === 2) {
      readDB((err, db) => {
        if (err) {
          res.writeHead(500, { 'Content-Type': 'application/json' });
          res.end(JSON.stringify({ error: 'Failed to read database' }));
          return;
        }
        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify(db.transactions));
      });
      return;
    }
    // POST /api/transactions => add a new transaction
    else if (method === 'POST' && parts.length === 2) {
      parseRequestBody(req, (err, newTx) => {
        if (err) {
          res.writeHead(400, { 'Content-Type': 'application/json' });
          res.end(JSON.stringify({ error: 'Invalid JSON body' }));
          return;
        }
        // Validate transaction schema before proceeding
        if (!validateTransaction(newTx)) {
          res.writeHead(400, { 'Content-Type': 'application/json' });
          res.end(JSON.stringify({ error: 'Invalid transaction schema' }));
          return;
        }
        readDB((err, db) => {
          if (err) {
            res.writeHead(500, { 'Content-Type': 'application/json' });
            res.end(JSON.stringify({ error: 'Failed to read database' }));
            return;
          }
          // Generate a new transaction id (starting at 1001 if none exist)
          newTx.id = db.transactions && db.transactions.length > 0 
            ? Math.max(...db.transactions.map(tx => tx.id)) + 1 
            : 1001;
          
          // Push the new transaction into transactions array
          db.transactions.push(newTx);
          
          // Update the user's transactions array using newTx.userId
          const userIndex = db.users.findIndex(u => u.id === newTx.userId);
          if (userIndex !== -1) {
            if (!Array.isArray(db.users[userIndex].transactions)) {
              db.users[userIndex].transactions = [];
            }
            db.users[userIndex].transactions.push(newTx.id);
          }
          
          writeDB(db, (err) => {
            if (err) {
              res.writeHead(500, { 'Content-Type': 'application/json' });
              res.end(JSON.stringify({ error: 'Failed to save new transaction' }));
              return;
            }
            res.writeHead(201, { 'Content-Type': 'application/json' });
            res.end(JSON.stringify(newTx));
          });
        });
      });
      return;
    } else {
      res.writeHead(405, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ error: 'Method not allowed' }));
      return;
    }
  }

  // --- Endpoints for Plans (existing) ---
  if (parsedUrl.pathname.startsWith('/api/plans')) {
    const parts = parsedUrl.pathname.replace(/^\/+|\/+$/g, '').split('/');
    if (parts[0] === 'api' && parts[1] === 'plans') {
      // GET /api/plans => return all plans
      if (method === 'GET' && parts.length === 2) {
        readDB((err, db) => {
          if (err) {
            res.writeHead(500, { 'Content-Type': 'application/json' });
            res.end(JSON.stringify({ error: 'Failed to read database' }));
            return;
          }
          res.writeHead(200, { 'Content-Type': 'application/json' });
          res.end(JSON.stringify(db.plans));
        });
      }
      // POST /api/plans => add a new plan
      else if (method === 'POST' && parts.length === 2) {
        parseRequestBody(req, (err, newPlan) => {
          if (err) {
            res.writeHead(400, { 'Content-Type': 'application/json' });
            res.end(JSON.stringify({ error: 'Invalid JSON body' }));
            return;
          }
          readDB((err, db) => {
            if (err) {
              res.writeHead(500, { 'Content-Type': 'application/json' });
              res.end(JSON.stringify({ error: 'Failed to read database' }));
              return;
            }
            newPlan.id = db.plans.length > 0 ? Math.max(...db.plans.map(p => p.id)) + 1 : 1;
            db.plans.push(newPlan);
            writeDB(db, (err) => {
              if (err) {
                res.writeHead(500, { 'Content-Type': 'application/json' });
                res.end(JSON.stringify({ error: 'Failed to save new plan' }));
                return;
              }
              res.writeHead(201, { 'Content-Type': 'application/json' });
              res.end(JSON.stringify(newPlan));
            });
          });
        });
      }
      // PUT /api/plans/:id => update an existing plan
      else if (method === 'PUT' && parts.length === 3) {
        const planId = parseInt(parts[2], 10);
        parseRequestBody(req, (err, updatedData) => {
          if (err) {
            res.writeHead(400, { 'Content-Type': 'application/json' });
            res.end(JSON.stringify({ error: 'Invalid JSON body' }));
            return;
          }
          readDB((err, db) => {
            if (err) {
              res.writeHead(500, { 'Content-Type': 'application/json' });
              res.end(JSON.stringify({ error: 'Failed to read database' }));
              return;
            }
            const index = db.plans.findIndex(plan => plan.id === planId);
            if (index === -1) {
              res.writeHead(404, { 'Content-Type': 'application/json' });
              res.end(JSON.stringify({ error: 'Plan not found' }));
              return;
            }
            db.plans[index] = { ...db.plans[index], ...updatedData };
            writeDB(db, (err) => {
              if (err) {
                res.writeHead(500, { 'Content-Type': 'application/json' });
                res.end(JSON.stringify({ error: 'Failed to update plan' }));
                return;
              }
              res.writeHead(200, { 'Content-Type': 'application/json' });
              res.end(JSON.stringify(db.plans[index]));
            });
          });
        });
      }
      // DELETE /api/plans/:id => delete a plan
      else if (method === 'DELETE' && parts.length === 3) {
        const planId = parseInt(parts[2], 10);
        readDB((err, db) => {
          if (err) {
            res.writeHead(500, { 'Content-Type': 'application/json' });
            res.end(JSON.stringify({ error: 'Failed to read database' }));
            return;
          }
          const originalLength = db.plans.length;
          db.plans = db.plans.filter(plan => plan.id !== planId);
          if (db.plans.length === originalLength) {
            res.writeHead(404, { 'Content-Type': 'application/json' });
            res.end(JSON.stringify({ error: 'Plan not found' }));
            return;
          }
          writeDB(db, (err) => {
            if (err) {
              res.writeHead(500, { 'Content-Type': 'application/json' });
              res.end(JSON.stringify({ error: 'Failed to delete plan' }));
              return;
            }
            res.writeHead(200, { 'Content-Type': 'application/json' });
            res.end(JSON.stringify({ message: 'Plan deleted successfully' }));
          });
        });
      } else {
        res.writeHead(405, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ error: 'Method not allowed' }));
      }
    } else {
      res.writeHead(404, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ error: 'Not found' }));
    }
  }
  // Serve Admin.html if requested
  else if (parsedUrl.pathname === '/Admin.html' && method === 'GET') {
    fs.readFile(path.join(__dirname, 'Admin.html'), 'utf8', (err, content) => {
      if (err) {
        res.writeHead(500, { 'Content-Type': 'text/plain' });
        res.end('Error loading Admin.html');
        return;
      }
      res.writeHead(200, { 'Content-Type': 'text/html' });
      res.end(content);
    });
  }
  // Serve Guest.html if requested
  else if (parsedUrl.pathname === '/Guest.html' && method === 'GET') {
    fs.readFile(path.join(__dirname, 'Guest.html'), 'utf8', (err, content) => {
      if (err) {
        res.writeHead(500, { 'Content-Type': 'text/plain' });
        res.end('Error loading Guest.html');
        return;
      }
      res.writeHead(200, { 'Content-Type': 'text/html' });
      res.end(content);
    });
  }
  // Serve CSS files
  else if (parsedUrl.pathname.startsWith('/css/')) {
    const cssPath = path.join(__dirname, parsedUrl.pathname);
    fs.readFile(cssPath, (err, content) => {
      if (err) {
        res.writeHead(404, { 'Content-Type': 'text/plain' });
        res.end('CSS file not found');
        return;
      }
      res.writeHead(200, { 'Content-Type': 'text/css' });
      res.end(content);
    });
  }
  // Serve image files
  else if (parsedUrl.pathname.startsWith('/Assets/Images/')) {
    const requestedPath = decodeURIComponent(parsedUrl.pathname);
    const imgPath = path.join(__dirname, requestedPath);
    fs.readFile(imgPath, (err, content) => {
      if (err) {
        console.error("Error reading image file:", err);
        res.writeHead(404, { 'Content-Type': 'text/plain' });
        res.end('Image not found');
        return;
      }
      let contentType = 'image/jpeg';
      if (imgPath.endsWith('.png')) {
        contentType = 'image/png';
      } else if (imgPath.endsWith('.svg')) {
        contentType = 'image/svg+xml';
      } else if (imgPath.endsWith('.gif')) {
        contentType = 'image/gif';
      }
      res.writeHead(200, { 'Content-Type': contentType });
      res.end(content);
    });
  }
  // Serve HTML files from the /HTML folder
  else if (parsedUrl.pathname.startsWith('/HTML/')) {
    const filePath = path.join(__dirname, parsedUrl.pathname);
    fs.readFile(filePath, 'utf8', (err, content) => {
      if (err) {
        res.writeHead(404, { 'Content-Type': 'text/plain' });
        res.end('HTML file not found');
        return;
      }
      res.writeHead(200, { 'Content-Type': 'text/html' });
      res.end(content);
    });
  }
  // Fallback for any other requests
  else {
    res.writeHead(404, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ error: 'Not found' }));
  }
});

server.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
