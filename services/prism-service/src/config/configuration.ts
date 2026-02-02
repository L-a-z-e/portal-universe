export default () => ({
  port: parseInt(process.env.PORT || '8085', 10),
  database: {
    host: process.env.DB_HOST || 'localhost',
    port: parseInt(process.env.DB_PORT || '5432', 10),
    username: process.env.DB_USERNAME || 'prism',
    password: process.env.DB_PASSWORD || 'prism',
    database: process.env.DB_DATABASE || 'prism',
    synchronize: process.env.NODE_ENV !== 'production',
    logging: process.env.NODE_ENV !== 'production',
  },
  encryption: {
    key: process.env.ENCRYPTION_KEY || 'default-32-byte-encryption-key!!',
  },
  kafka: {
    brokers: (process.env.KAFKA_BROKERS || 'localhost:9092').split(','),
    clientId: 'prism-service',
  },
});
