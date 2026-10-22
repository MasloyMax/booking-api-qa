import { createServer } from 'node:http';
import { DatabaseSync } from 'node:sqlite';
import { randomUUID } from 'node:crypto';

const db = new DatabaseSync(':memory:');
db.exec(`
  CREATE TABLE bookings (
    id TEXT PRIMARY KEY, owner TEXT NOT NULL, room TEXT NOT NULL,
    start TEXT NOT NULL, end TEXT NOT NULL, status TEXT NOT NULL CHECK(status IN ('confirmed','cancelled'))
  );
  CREATE INDEX availability ON bookings(room, status, start, end);
  CREATE TABLE idempotency (
    owner TEXT NOT NULL, key TEXT NOT NULL, payload TEXT NOT NULL, response TEXT NOT NULL,
    PRIMARY KEY(owner, key)
  );
`);
const tokens = new Map();
const json = (res, code, body) => {
  res.writeHead(code, { 'content-type': 'application/json', 'cache-control': 'no-store' });
  res.end(JSON.stringify(body));
};
const fail = (res, code, error) => json(res, code, { error });
async function body(req) {
  let raw = '';
  for await (const chunk of req) {
    raw += chunk;
    if (raw.length > 16_384) throw new SyntaxError('body too large');
  }
  return JSON.parse(raw || '{}');
}
function validDate(value) {
  if (typeof value !== 'string' || !/^\d{4}-\d{2}-\d{2}$/.test(value)) return false;
  const date = new Date(value + 'T00:00:00Z');
  return !Number.isNaN(date.valueOf()) && date.toISOString().slice(0, 10) === value;
}
function validBooking(input) {
  return input && typeof input === 'object' && !Array.isArray(input) &&
    Object.keys(input).every(key => ['room', 'start', 'end'].includes(key)) &&
    typeof input.room === 'string' && /^[a-zA-Z0-9-]{1,80}$/.test(input.room) &&
    validDate(input.start) && validDate(input.end) && input.start < input.end;
}
const server = createServer(async (req, res) => {
  try {
    const url = new URL(req.url, 'http://localhost');
    const path = url.pathname;
    if (path === '/health' && req.method === 'GET') return json(res, 200, { status: 'ok' });
    if (path === '/auth/token' && req.method === 'POST') {
      const credentials = await body(req);
      if (!credentials || !['alice', 'bob'].includes(credentials.username) || credentials.password !== 'demo-password') {
        return fail(res, 401, 'invalid_credentials');
      }
      const token = randomUUID();
      tokens.set(token, { owner: credentials.username, expires: Date.now() + 900_000 });
      return json(res, 200, { access_token: token, token_type: 'Bearer', expires_in: 900 });
    }
    const match = req.headers.authorization?.match(/^Bearer (.+)$/);
    const principal = match && tokens.get(match[1]);
    if (!principal || principal.expires <= Date.now()) return fail(res, 401, 'unauthorized');
    const owner = principal.owner;
    if (path === '/bookings' && req.method === 'POST') {
      const input = await body(req);
      if (!validBooking(input)) return fail(res, 422, 'invalid_booking');
      const key = req.headers['idempotency-key'];
      if (typeof key !== 'string' || !/^[a-zA-Z0-9-]{1,100}$/.test(key)) return fail(res, 400, 'invalid_idempotency_key');
      // Canonical field order makes JSON object key order irrelevant.
      const payload = JSON.stringify({ room: input.room, start: input.start, end: input.end });
      db.exec('BEGIN IMMEDIATE');
      try {
        const previous = db.prepare('SELECT payload, response FROM idempotency WHERE owner = ? AND key = ?').get(owner, key);
        if (previous) {
          db.exec('COMMIT');
          if (previous.payload !== payload) return fail(res, 409, 'idempotency_conflict');
          return json(res, 200, JSON.parse(previous.response));
        }
        const overlap = db.prepare("SELECT id FROM bookings WHERE room = ? AND status = 'confirmed' AND start < ? AND end > ?").get(input.room, input.end, input.start);
        if (overlap) {
          db.exec('COMMIT');
          return fail(res, 409, 'room_unavailable');
        }
        const booking = { id: randomUUID(), owner, ...input, status: 'confirmed' };
        db.prepare('INSERT INTO bookings VALUES (?, ?, ?, ?, ?, ?)').run(booking.id, owner, input.room, input.start, input.end, booking.status);
        db.prepare('INSERT INTO idempotency VALUES (?, ?, ?, ?)').run(owner, key, payload, JSON.stringify(booking));
        db.exec('COMMIT');
        res.setHeader('location', '/bookings/' + booking.id);
        return json(res, 201, booking);
      } catch (error) {
        db.exec('ROLLBACK');
        throw error;
      }
    }
    if (path === '/bookings' && req.method === 'GET') {
      const room = url.searchParams.get('room');
      const rows = room
        ? db.prepare('SELECT * FROM bookings WHERE owner = ? AND room = ? ORDER BY id').all(owner, room)
        : db.prepare('SELECT * FROM bookings WHERE owner = ? ORDER BY id').all(owner);
      return json(res, 200, { items: rows });
    }
    const bookingMatch = path.match(/^\/bookings\/([a-f0-9-]+)$/);
    if (bookingMatch && ['GET', 'DELETE'].includes(req.method)) {
      const booking = db.prepare('SELECT * FROM bookings WHERE id = ? AND owner = ?').get(bookingMatch[1], owner);
      if (!booking) return fail(res, 404, 'booking_not_found');
      if (req.method === 'DELETE') {
        db.prepare("UPDATE bookings SET status = 'cancelled' WHERE id = ? AND owner = ?").run(booking.id, owner);
        booking.status = 'cancelled';
      }
      return json(res, 200, booking);
    }
    return fail(res, 404, 'route_not_found');
  } catch (error) {
    if (error instanceof SyntaxError) return fail(res, 400, 'invalid_json');
    console.error(error);
    return fail(res, 500, 'internal_error');
  }
});
server.listen(Number(process.env.PORT ?? 0), '127.0.0.1', () => {
  console.log(JSON.stringify({ port: server.address().port }));
});
function close() { server.close(() => { db.close(); process.exit(0); }); }
process.on('SIGTERM', close);
process.on('SIGINT', close);
