import * as Yup from 'yup';

export const registerSchema = Yup.object().shape({
  name: Yup.string().min(2, 'Too short').required('Required'),
  email: Yup.string().email('Invalid email').required('Required'),
  phoneNumber: Yup.string().matches(/^[0-9]{10}$/, 'Phone number must be 10 digits').required('Required'),
  password: Yup.string().min(6, 'Minimum 6 characters').required('Required'),
  role: Yup.string().oneOf(['USER', 'PROVIDER', 'ADMIN'], 'Invalid role').required('Required'),
});


export const loginSchema = Yup.object().shape({
  email: Yup.string().email('Invalid email').required('Required'),
  password: Yup.string().required('Required'),
});
