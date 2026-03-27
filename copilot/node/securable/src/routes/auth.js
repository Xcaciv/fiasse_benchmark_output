'use strict';

const express = require('express');
const router = express.Router();
const authController = require('../controllers/authController');
const { registerValidator, loginValidator, forgotPasswordValidator, resetPasswordValidator } = require('../validators/authValidators');
const { authLimiter } = require('../middleware/rateLimiter');

router.get('/register', authController.getRegisterPage);
router.post('/register', authLimiter, registerValidator, authController.postRegister);

router.get('/login', authController.getLoginPage);
router.post('/login', authLimiter, loginValidator, authController.postLogin);

router.post('/logout', authController.postLogout);

router.get('/forgot-password', authController.getForgotPasswordPage);
router.post('/forgot-password', forgotPasswordValidator, authController.postForgotPassword);

router.get('/reset-password/:token', authController.getResetPasswordPage);
router.post('/reset-password/:token', resetPasswordValidator, authController.postResetPassword);

module.exports = router;
