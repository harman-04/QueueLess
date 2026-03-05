import * as Yup from 'yup';

const TRUSTED_DOMAINS = ['gmail.com', 'yahoo.com', 'outlook.com', 'hotmail.com'];

export const registerSchema = Yup.object().shape({
  name: Yup.string()
    .min(2, 'Name must be at least 2 characters')
    .required('Name is required'),
  email: Yup.string()
    .email('Invalid email address')
    .required('Email is required')
    .test('is-trusted-domain', 'Only trusted email providers (gmail, yahoo, outlook, hotmail) are allowed.', (value) => {
      if (!value) return false;
      const domain = value.split('@')[1];
      return TRUSTED_DOMAINS.includes(domain);
    }),
  phoneNumber: Yup.string()
    .matches(
      /^\+?[0-9\s-]{10,15}$/,
      'Phone number must be 10-15 digits and may include +, spaces, or hyphens'
    )
    .required('Phone number is required'),
  password: Yup.string()
    .min(8, 'Password must be at least 8 characters')
    .matches(
      /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).*$/,
      'Password must contain at least one uppercase letter, one lowercase letter, and one number'
    )
    .required('Password is required'),
  role: Yup.string()
    .oneOf(['USER', 'PROVIDER', 'ADMIN'], 'Invalid role')
    .required('Role is required'),
});

export const loginSchema = Yup.object().shape({
  email: Yup.string().email('Invalid email').required('Email is required'),
  password: Yup.string().required('Password is required'),
});