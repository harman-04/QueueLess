//src/validation/authSchema.js
import * as Yup from 'yup';

const TRUSTED_DOMAINS = ['gmail.com', 'yahoo.com', 'outlook.com', 'hotmail.com'];

export const registerSchema = Yup.object().shape({
  name: Yup.string().min(2, 'Too short').required('Required'),
  email: Yup.string()
    .email('Invalid email address')
    .required('Required')
    .test('is-trusted-domain', 'Only trusted email providers like google, yahoo, and outlook are allowed.', (value) => {
      if (!value) return false;
      const domain = value.split('@')[1];
      return TRUSTED_DOMAINS.includes(domain);
    }),
  phoneNumber: Yup.string().matches(/^[0-9]{10}$/, 'Phone number must be 10 digits').required('Required'),
  password: Yup.string().min(6, 'Minimum 6 characters').required('Required'),
  role: Yup.string().oneOf(['USER', 'PROVIDER', 'ADMIN'], 'Invalid role').required('Required'),
});


export const loginSchema = Yup.object().shape({
  email: Yup.string().email('Invalid email').required('Required'),
  password: Yup.string().required('Required'),
});
