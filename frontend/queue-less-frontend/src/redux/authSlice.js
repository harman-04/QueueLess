import { createSlice } from '@reduxjs/toolkit';

const token = localStorage.getItem('token');
const role = localStorage.getItem('role');
const id = localStorage.getItem('userId'); // ✅ Change variable name
const name = localStorage.getItem('name');

const authSlice = createSlice({
  name: 'auth',
  initialState: {
    token: token || null,
    role: role || null,
    id: id || null, // ✅ Change state key from 'user' to 'id'
    name: name || null,
  },
  reducers: {
    loginSuccess: (state, action) => {
      state.token = action.payload.token;
      state.role = action.payload.role;
      state.id = action.payload.userId; // ✅ Change state key from 'user' to 'id'
      state.name = action.payload.name;
      localStorage.setItem('token', action.payload.token);
      localStorage.setItem('role', action.payload.role);
      localStorage.setItem('userId', action.payload.userId);
      localStorage.setItem('name', action.payload.name);
    },
    logout: (state) => {
      state.token = null;
      state.role = null;
      state.id = null; // ✅ Change state key from 'user' to 'id'
      state.name = null;
      localStorage.clear();
    },
  },
});

export const { loginSuccess, logout } = authSlice.actions;
export default authSlice.reducer;