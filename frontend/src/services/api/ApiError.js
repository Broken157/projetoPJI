export default class ApiError extends Error {
  constructor({ status, message, body = null }) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.body = body;
  }
}
