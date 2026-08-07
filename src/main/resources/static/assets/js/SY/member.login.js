$(function () {
  const csrfToken = $('meta[name="_csrf"]').attr('content');
  const csrfHeader = $('meta[name="_csrf_header"]').attr('content');
  const EMAIL_BUTTON_CHECK = '중복확인';
  const EMAIL_BUTTON_SEND = '인증번호전송';
  const PASSWORD_PATTERN = /^[A-Za-z0-9!@#$%^&*]{8,20}$/;
  const SIGNUP_LEAVE_MESSAGE = '회원가입 진행 중입니다. 뒤로 갈 시 초기화됩니다.';
  const REMEMBER_EMAIL_KEY = 'teamSyncRememberEmail';

  let emailChecked = false;
  let emailCodeSent = false;
  let emailVerified = false;
  let checkedEmail = '';
  let verifiedEmail = '';
  let phoneChecked = false;
  let checkedPhone = '';
  let timerId = null;
  let remainSeconds = 0;
  let resetTimerId = null;
  let resetRemainSeconds = 0;
  let resetPasswordEmail = '';
  let resetPasswordVerified = false;
  let signupHistoryActive = false;

  if (csrfToken && csrfHeader) {
    $.ajaxSetup({
      beforeSend: function (xhr) {
        xhr.setRequestHeader(csrfHeader, csrfToken);
      }
    });
  }

  if ($('#sy-signup-server-error').length > 0) {
    showSignupPaneAfterServerError();
  }

  restoreRememberedEmail();

  $('.sy-tab').on('click', function () {
    const target = $(this).data('target');

    $('.sy-tab').removeClass('sy-on');
    $('.sy-pane').removeClass('sy-on');

    $(this).addClass('sy-on');
    $('#' + target).addClass('sy-on');

    if (target === 'sy-login') {
      showAuthView('login');
    }
  });

  $('#sy-show-find-email').on('click', function () {
    showAuthView('findEmail');
  });

  $('#sy-show-reset-password').on('click', function () {
    showAuthView('resetPassword');
  });

  $('#sy-login-back').on('click', function () {
    if ($('#sy-login').hasClass('sy-auth-subview-open')) {
      showAuthView('login');
      return;
    }

    if (window.history.length > 1) {
      window.history.back();
      return;
    }

    window.location.href = '/';
  });
   
  $('#sy-show-signup').on('click', function () {
    openSignupPane();
  });

  $('#sy-signup-back-login').on('click', function () {
    confirmSignupLeave();
  });

  $(window).on('popstate', function () {
    if (!$('#sy-signup').hasClass('sy-on')) {
      return;
    }

    if (confirmSignupLeave()) {
      signupHistoryActive = false;
      return;
    }

    signupHistoryActive = false;
    pushSignupHistoryState();
  });

  $(window).on('pageshow', function (event) {
    if (event.originalEvent && event.originalEvent.persisted) {
      resetSignupFlow();
      showLoginPane();
    }
  });

  $('#sy-btn-google-login').on('click', function () {
    window.location.href = '/oauth2/authorization/google';
  });

  $('.sy-term-view').on('click', function () {
    return false;
  });

  $('.sy-field input, .sy-field textarea').on('input', function () {
    clearInvalid($(this));
  });

  $('#sy-signup-phone, #sy-find-phone').on('input', function () {
    this.value = this.value.replace(/\D/g, '').slice(0, 11);

    if (this.id === 'sy-signup-phone') {
      resetPhoneCheck();
    }
  });
  
  $('#sy-check-phone').on('click', function () {
    const phone = $('#sy-signup-phone').val().trim();
    checkPhoneDuplicate(phone);
  });

  $('#sy-signup-birth, #sy-find-birth').on('input', function () {
    this.value = this.value.replace(/\D/g, '').slice(0, 8);
  });

  $('#sy-reset-email').on('input', function () {
    resetResetPasswordVerification();
  });

  $('#sy-login-form').on('submit', function (event) {
    const email = $('#sy-login-email').val().trim();
    const passwd = $('#sy-login-passwd').val().trim();

    if (!validateEmail($('#sy-login-email'), email) || !requireValue($('#sy-login-passwd'), passwd)) {
      event.preventDefault();
      saveRememberedEmail();
      return;
    }

    saveRememberedEmail();
  });

  $('#sy-agree-all').on('change', function () {
    const checked = $(this).is(':checked');
    $('.sy-required-term, .sy-optional-term').prop('checked', checked);
    clearInvalid($('.sy-terms'));
  });

  $('.sy-required-term, .sy-optional-term').on('change', function () {
    const total = $('.sy-required-term, .sy-optional-term').length;
    const checked = $('.sy-required-term:checked, .sy-optional-term:checked').length;

    $('#sy-agree-all').prop('checked', total === checked);
    clearInvalid($('.sy-terms'));
  });

  $('#sy-signup-email').on('input', function () {
    resetEmailVerification();
  });

  $('#sy-btn-check-email').on('click', function () {
    const email = $('#sy-signup-email').val().trim();

    if (!emailChecked) {
      checkEmailDuplicate(email);
      return;
    }

    if (checkedEmail !== email) {
      resetEmailVerification();
      checkEmailDuplicate(email);
      return;
    }

    sendEmailCode(email);
  });

  $('#sy-btn-verify-email').on('click', function () {
    const email = $('#sy-signup-email').val().trim();
    const code = $('#sy-email-code').val().trim();

    if (!isEmailFormat(email)) {
      markInvalid($('#sy-signup-email'), '이메일 형식으로 입력해주세요.');
      return;
    }

    if (!emailChecked || checkedEmail !== email) {
      setEmailMessage('', '');
      markInvalid($('#sy-signup-email'), '이메일 중복확인을 진행해주세요.');
      return;
    }

    if (!emailCodeSent) {
      setSignupCodeError('인증번호전송을 먼저 눌러주세요.');
      return;
    }

    if (remainSeconds <= 0) {
      expireEmailVerification();
      return;
    }

    if (code.length === 0) {
      setSignupCodeError('인증번호를 입력해주세요.');
      return;
    }

    $.ajax({
      url: '/users/verifyEmailCode',
      type: 'POST',
      data: {
        email: email,
        code: code
      },
      dataType: 'json',
      success: function (res) {
        if (res.result !== 'success') {
          if (res.result === 'expired') {
            expireEmailVerification();
          } else {
            setSignupCodeError(res.message || '이메일 인증에 실패했습니다.');
          }

          return;
        }

        emailVerified = true;
        verifiedEmail = email;

        stopEmailTimer();
        setSignupCodeOk('이메일 인증이 완료되었습니다.');

        $('#sy-email-code').prop('readonly', true);
        $('#sy-btn-verify-email').prop('disabled', true);
      },
      error: function () {
        setSignupCodeError('이메일 인증 확인 중 오류가 발생했습니다.');
      }
    });
  });

  $('#sy-btn-find-email').on('click', function () {
    const realName = $('#sy-find-real-name').val().trim();
    const phone = $('#sy-find-phone').val().trim();
    const birthText = $('#sy-find-birth').val().trim();

    if (!requireValue($('#sy-find-real-name'), realName, '이름을 입력해주세요.')) return;
    if (!validatePhone($('#sy-find-phone'), phone, '휴대폰번호는 숫자만 10~11자리로 입력해주세요.')) return;
    if (!validateBirth($('#sy-find-birth'), birthText, '생년월일은 8자리 숫자로 입력해주세요.')) return;

    $.ajax({
      url: '/users/findEmail',
      type: 'POST',
      data: {
        real_name: realName,
        phone: phone,
        birthText: birthText
      },
      dataType: 'json',
      success: function (res) {
        if (res.result !== 'success') {
          setFindEmailMessage(res.message || '이메일 찾기에 실패했습니다.', 'error');
          return;
        }

        setFindEmailMessage('가입된 이메일은 ' + res.email + ' 입니다.', 'ok');
      },
      error: function () {
        setFindEmailMessage('이메일 찾기 중 오류가 발생했습니다.', 'error');
      }
    });
  });

  $('#sy-btn-send-reset-code').on('click', function () {
    const email = $('#sy-reset-email').val().trim();

    resetPasswordVerified = false;
    resetPasswordEmail = '';

    if (!validateEmail($('#sy-reset-email'), email, '이메일 형식으로 입력해주세요.')) return;

    clearInvalid($('#sy-reset-email'));
    showResetPasswordCodeSending(email);

    $.ajax({
      url: '/users/sendResetPasswordCode',
      type: 'POST',
      data: { email: email },
      dataType: 'json',
      success: function (res) {
        if (res.result !== 'success') {
          rollbackResetPasswordCodeSending();
          $('#sy-reset-email').addClass('sy-input-error').focus();
          setResetEmailMessage(res.message || '인증번호 발송에 실패했습니다.', 'error');
          return;
        }

        resetPasswordEmail = email;
        setResetEmailMessage('인증번호를 이메일로 전송했습니다.', 'ok');
      },
      error: function () {
        rollbackResetPasswordCodeSending();
        setResetEmailMessage('인증번호 발송 중 오류가 발생했습니다.', 'error');
      }
    });
  });

  $('#sy-btn-verify-reset-code').on('click', function () {
    const email = $('#sy-reset-email').val().trim();
    const code = $('#sy-reset-code').val().trim();

    if (!validateEmail($('#sy-reset-email'), email, '이메일 형식으로 입력해주세요.')) return;
    if (!requireValue($('#sy-reset-code'), code, '인증번호를 입력해주세요.')) return;

    $.ajax({
      url: '/users/verifyResetPasswordCode',
      type: 'POST',
      data: {
        email: email,
        code: code
      },
      dataType: 'json',
      success: function (res) {
        if (res.result !== 'success') {
          resetPasswordVerified = false;
          $('#sy-reset-code').addClass('sy-input-error').focus();
          setResetCodeMessage(res.message || '인증번호 확인에 실패했습니다.', 'error');
          return;
        }

        resetPasswordVerified = true;
        resetPasswordEmail = email;
        clearInvalid($('#sy-reset-code'));
        setResetCodeMessage('이메일 인증이 완료되었습니다.', 'ok');
        stopResetTimer();
        $('#sy-reset-timer').text('');
        $('#sy-reset-passwd').focus();
      },
      error: function () {
        resetPasswordVerified = false;
        $('#sy-reset-code').addClass('sy-input-error').focus();
        setResetCodeMessage('인증번호 확인 중 오류가 발생했습니다.', 'error');
      }
    });
  });

  $('#sy-btn-reset-password').on('click', function () {
    const email = $('#sy-reset-email').val().trim();
    const passwd = $('#sy-reset-passwd').val().trim();
    const confirmPasswd = $('#sy-reset-confirm-passwd').val().trim();

    if (!validateEmail($('#sy-reset-email'), email, '이메일 형식으로 입력해주세요.')) return;

    if (!resetPasswordVerified || resetPasswordEmail !== email) {
      if (!resetPasswordEmail || resetPasswordEmail !== email) {
        markInvalid($('#sy-reset-email'), '이메일 인증을 진행해주세요.');
        return;
      }

      $('#sy-reset-code-info').addClass('sy-show');
      $('#sy-reset-code').addClass('sy-input-error').focus();
      setResetCodeMessage('이메일 인증을 완료해주세요.', 'error');
      return;
    }

    if (!validatePassword($('#sy-reset-passwd'), passwd, '비밀번호는 8~20자 영문, 숫자, 특수문자 조합으로 입력해주세요.')) return;

    if (!requireValue($('#sy-reset-confirm-passwd'), confirmPasswd, '새 비밀번호 확인을 입력해주세요.')) return;

    if (passwd !== confirmPasswd) {
      markInvalid($('#sy-reset-confirm-passwd'), '비밀번호가 일치하지 않습니다.');
      return;
    }

    $.ajax({
      url: '/users/resetPassword',
      type: 'POST',
      data: {
        email: email,
        passwd: passwd,
        confirm_passwd: confirmPasswd
      },
      dataType: 'json',
      success: function (res) {
        if (res.result !== 'success') {
          handleResetPasswordFailure(res);
          return;
        }

        resetPasswordVerified = false;
        resetPasswordEmail = '';
        $('#sy-reset-code, #sy-reset-passwd, #sy-reset-confirm-passwd').val('');
        stopResetTimer();
        $('#sy-reset-code-info').removeClass('sy-show');
        $('#sy-reset-timer').text('05:00');
        showAuthView('login');
        showLoginMessage(res.message || '비밀번호가 변경되었습니다. 다시 로그인해주세요.', 'success');
        $('#sy-login-email').val(email).focus();
      },
      error: function () {
        markInvalid($('#sy-reset-confirm-passwd'), '비밀번호 변경 중 오류가 발생했습니다.');
      }
    });
  });

  $('#sy-signup-email-next').on('click', function () {
    if (!validateSignupEmailStep()) return;

    showSignupStep('account');
    $('#sy-signup-real-name').focus();
  });

  $('#sy-signup-next').on('click', function () {
    if (!validateSignupAccountStep()) return;

    showSignupStep('profile');
    $('#sy-signup-user-name').focus();
  });

  $('#sy-signup-form').on('submit', function (event) {
    if (!validateSignupEmailStep() || !validateSignupAccountStep() || !validateSignupProfileStep()) {
      event.preventDefault();
    }
  });

  function validateSignupEmailStep() {
    const email = $('#sy-signup-email').val().trim();

    if (!validateEmail($('#sy-signup-email'), email, '이메일 형식으로 입력해주세요.')) return false;

    if (!emailChecked || checkedEmail !== email) {
      setEmailMessage('', '');
      markInvalid($('#sy-signup-email'), '이메일 중복확인을 진행해주세요.');
      return false;
    }

    if (!emailCodeSent) {
      setSignupCodeError('인증번호를 전송해주세요.');
      return false;
    }

    if (remainSeconds <= 0) {
      expireEmailVerification();
      return false;
    }

    if (!emailVerified || verifiedEmail !== email) {
      setSignupCodeError('이메일 인증을 완료해주세요.');
      return false;
    }

    return true;
  }

  function validateSignupAccountStep() {
    const realName = $('#sy-signup-real-name').val().trim();
    const phone = $('#sy-signup-phone').val().trim();
    const birthText = $('#sy-signup-birth').val().trim();
    const passwd = $('#sy-signup-passwd').val().trim();
    const confirmPasswd = $('#sy-signup-confirm-passwd').val().trim();

    if (!requireValue($('#sy-signup-real-name'), realName, '이름을 입력해주세요.')) return false;
    if (!validatePhone($('#sy-signup-phone'), phone, '휴대폰번호는 숫자만 10~11자리로 입력해주세요.')) return false;
	if (!phoneChecked || checkedPhone !== phone) {
	  markInvalid($('#sy-signup-phone'), '휴대폰 번호 중복검사를 진행해주세요.');
	  setPhoneMessage('휴대폰 번호 중복검사를 진행해주세요.', 'error');
	  return false;
	}
    if (!validateBirth($('#sy-signup-birth'), birthText, '생년월일은 8자리 숫자로 입력해주세요.')) return false;
    if (!validatePassword($('#sy-signup-passwd'), passwd, '비밀번호는 8~20자 영문, 숫자, 특수문자 조합으로 입력해주세요.')) return false;

    if (passwd !== confirmPasswd) {
      markInvalid($('#sy-signup-confirm-passwd'), '비밀번호가 일치하지 않습니다.');
      return false;
    }

    return true;
  }

  function validateSignupProfileStep() {
    const userName = $('#sy-signup-user-name').val().trim();
    const intro = $('#sy-signup-intro').val().trim();

    if (!requireValue($('#sy-signup-user-name'), userName, '닉네임을 입력해주세요.')) return false;
    if (!requireValue($('#sy-signup-intro'), intro, '자기소개를 입력해주세요.')) return false;

    if ($('.sy-required-term:checked').length !== $('.sy-required-term').length) {
      markInvalid($('.sy-terms'), '필수 약관에 동의해주세요.');
      return false;
    }

    return true;
  }
  
  function checkPhoneDuplicate(phone) {
    resetPhoneCheck();

    if (!validatePhone($('#sy-signup-phone'), phone, '휴대폰번호는 숫자만 10~11자리로 입력해주세요.')) {
      setPhoneMessage('휴대폰번호는 숫자만 10~11자리로 입력해주세요.', 'error');
      return;
    }

    $('#sy-check-phone').prop('disabled', true);

    $.ajax({
      url: '/users/checkPhone',
      type: 'GET',
      data: { phone: phone },
      dataType: 'json',
      success: function (res) {
        $('#sy-check-phone').prop('disabled', false);

        if (res.duplicated || res.result === 'duplicated') {
          phoneChecked = false;
          checkedPhone = '';
          markInvalid($('#sy-signup-phone'));
          setPhoneMessage(res.message || '이미 사용 중인 휴대폰 번호입니다.', 'error');
          return;
        }

        phoneChecked = true;
        checkedPhone = phone;

        clearInvalid($('#sy-signup-phone'));
        setPhoneMessage(res.message || '사용 가능한 휴대폰 번호입니다.', 'ok');
      },
      error: function () {
        $('#sy-check-phone').prop('disabled', false);
        setPhoneMessage('휴대폰 번호 중복 확인 중 오류가 발생했습니다.', 'error');
      }
    });
  }

  function resetPhoneCheck() {
    phoneChecked = false;
    checkedPhone = '';
    setPhoneMessage('', '');
  }

  function setPhoneMessage(message, type) {
    setStateMessage($('#sy-phone-check-msg'), message, type);
  }

  function checkEmailDuplicate(email) {
    resetEmailVerification();

    if (!validateEmail($('#sy-signup-email'), email, '이메일 형식으로 입력해주세요.')) return;

    $.ajax({
      url: '/users/checkEmail',
      type: 'GET',
      data: { email: email },
      dataType: 'json',
      success: function (res) {
        if (res.duplicated) {
          markInvalid($('#sy-signup-email'));
          setEmailMessage('이미 가입한 이메일입니다.', 'error');
          return;
        }

        emailChecked = true;
        checkedEmail = email;

        clearInvalid($('#sy-signup-email'));
        $('#sy-btn-check-email').text(EMAIL_BUTTON_SEND);
        setEmailMessage('사용 가능한 이메일입니다. 인증번호를 발송해주세요.', 'ok');
      },
      error: function () {
        setEmailMessage('이메일 중복 확인 중 오류가 발생했습니다.', 'error');
      }
    });
  }

  function sendEmailCode(email) {
    if (!validateEmail($('#sy-signup-email'), email)) return;

    if (!emailChecked || checkedEmail !== email) {
      resetEmailVerification();
      markInvalid($('#sy-signup-email'));
      setEmailMessage('이메일 중복확인을 진행해주세요.', 'error');
      return;
    }

    emailCodeSent = false;
    emailVerified = false;
    verifiedEmail = '';

    $('#sy-btn-check-email').prop('disabled', true);
    showEmailCodeSending();

    $.ajax({
      url: '/users/sendEmailCode',
      type: 'POST',
      data: { email: email },
      dataType: 'json',
      success: function (res) {
        $('#sy-btn-check-email').prop('disabled', false);

        if (res.result === 'duplicated') {
          resetEmailVerification();
          markInvalid($('#sy-signup-email'));
          setEmailMessage('이미 가입한 이메일입니다.', 'error');
          return;
        }

        if (res.result !== 'success') {
          rollbackEmailCodeSending();
          setEmailMessage(res.message || '이메일 인증번호 발송에 실패했습니다.', 'error');
          return;
        }

        emailCodeSent = true;
        emailVerified = false;
        verifiedEmail = '';

        setEmailMessage('인증번호를 발송했습니다.', 'ok');
        setCodeMessage('메일로 전송된 인증번호를 입력해주세요.', '');
      },
      error: function () {
        $('#sy-btn-check-email').prop('disabled', false);
        rollbackEmailCodeSending();
        setEmailMessage('이메일 인증번호 발송 중 오류가 발생했습니다.', 'error');
      }
    });
  }

  function showEmailCodeSending() {
    emailCodeSent = true;
    emailVerified = false;
    verifiedEmail = '';

    setEmailMessage('인증번호를 발송했습니다.', 'ok');
    setCodeMessage('메일로 전송된 인증번호를 입력해주세요.', '');

    $('#sy-email-verification-box').addClass('sy-show');
    $('#sy-email-code-msg').closest('.sy-code-info').addClass('sy-show');
    $('#sy-email-code').val('').prop('readonly', false).focus();
    clearSignupCodeState();
    $('#sy-btn-verify-email').prop('disabled', false);

    startEmailTimer(300);
  }

  function rollbackEmailCodeSending() {
    emailCodeSent = false;
    emailVerified = false;
    verifiedEmail = '';

    stopEmailTimer();

    $('#sy-btn-check-email')
      .text(EMAIL_BUTTON_SEND)
      .prop('disabled', false);

    $('#sy-email-code-msg').closest('.sy-code-info').removeClass('sy-show');
    $('#sy-email-code').val('').prop('readonly', false);
    $('#sy-btn-verify-email').prop('disabled', false);
    $('#sy-email-timer').text('05:00');
  }

  function resetEmailVerification() {
    emailChecked = false;
    emailCodeSent = false;
    emailVerified = false;
    checkedEmail = '';
    verifiedEmail = '';

    stopEmailTimer();

    $('#sy-btn-check-email')
      .text(EMAIL_BUTTON_CHECK)
      .prop('disabled', false);

    setEmailMessage('', '');
    setCodeMessage('메일로 전송된 인증번호를 입력해주세요.', '');

    $('#sy-email-verification-box').addClass('sy-show');
    $('#sy-email-code-msg').closest('.sy-code-info').removeClass('sy-show');
    $('#sy-email-code').val('').prop('readonly', false);
    $('#sy-btn-verify-email').prop('disabled', false);
    $('#sy-email-timer').text('05:00');
  }

  function expireEmailVerification() {
    emailCodeSent = false;
    emailVerified = false;
    verifiedEmail = '';

    stopEmailTimer();

    $('#sy-btn-check-email')
      .text(EMAIL_BUTTON_SEND)
      .prop('disabled', false);

    $('#sy-email-code').prop('readonly', false);
    $('#sy-btn-verify-email').prop('disabled', false);
    $('#sy-email-timer').text('00:00');
    $('#sy-email-code-msg').closest('.sy-code-info').addClass('sy-show');

    setSignupCodeError('인증 시간이 만료되었습니다. 이메일 인증을 다시 진행해주세요.');
  }

  function showResetPasswordCodeSending(email) {
    resetPasswordVerified = false;
    resetPasswordEmail = email;

    setResetEmailMessage('인증번호를 이메일로 전송했습니다.', 'ok');

    $('#sy-reset-code').val('').prop('readonly', false).focus();
    $('#sy-btn-verify-reset-code').prop('disabled', false);
    $('#sy-reset-code-info').addClass('sy-show');
    clearInvalid($('#sy-reset-code'));
    setResetCodeMessage('메일로 전송된 인증번호를 입력해주세요.', '');

    startResetTimer(300);
  }

  function rollbackResetPasswordCodeSending() {
    resetPasswordVerified = false;
    resetPasswordEmail = '';

    stopResetTimer();

    $('#sy-reset-code').val('').prop('readonly', false);
    $('#sy-btn-verify-reset-code').prop('disabled', false);
    $('#sy-reset-code-info').removeClass('sy-show');
    $('#sy-reset-timer').text('05:00');
  }

  function resetResetPasswordVerification() {
    resetPasswordVerified = false;
    resetPasswordEmail = '';

    stopResetTimer();

    setResetEmailMessage('', '');
    setResetCodeMessage('메일로 전송된 인증번호를 입력해주세요.', '');

    $('#sy-reset-code, #sy-reset-passwd, #sy-reset-confirm-passwd')
      .val('')
      .removeClass('sy-input-error');
    $('#sy-reset-code').prop('readonly', false);
    $('#sy-btn-verify-reset-code').prop('disabled', false);
    $('#sy-reset-code-info').removeClass('sy-show');
    $('#sy-reset-timer').text('05:00');

    $('#sy-reset-password-view').find('.sy-field-error').remove();
  }

  function handleResetPasswordFailure(res) {
    const result = res && res.result;
    const message = (res && res.message) || '비밀번호 변경에 실패했습니다.';

    if (result === 'notVerified' || result === 'expired') {
      resetPasswordVerified = false;
      stopResetTimer();
      $('#sy-reset-code-info').addClass('sy-show');
      $('#sy-reset-code').addClass('sy-input-error').focus();
      setResetCodeMessage(message, 'error');
      if (result === 'expired') {
        $('#sy-reset-timer').text('00:00');
      }
      return;
    }

    if (result === 'invalidPassword' || result === 'empty') {
      markInvalid($('#sy-reset-passwd'), message);
      return;
    }

    if (result === 'passwordMismatch') {
      markInvalid($('#sy-reset-confirm-passwd'), message);
      return;
    }

    if (result === 'unavailable' || result === 'socialAccount' || result === 'notFound') {
      markInvalid($('#sy-reset-email'), message);
      return;
    }

    markInvalid($('#sy-reset-confirm-passwd'), message);
  }

  function showAuthView(view) {
    $('.sy-auth-view').removeClass('sy-show');
    $('#sy-login').toggleClass('sy-auth-subview-open', view !== 'login');
    $('#sy-login > .sy-msg').toggle(view === 'login');

    if (view !== 'login') {
      clearLoginMessage();
    }

    if (view === 'findEmail') {
      $('#sy-find-email-view').addClass('sy-show');
      $('#sy-find-real-name').focus();
      return;
    }

    if (view === 'resetPassword') {
      $('#sy-reset-password-view').addClass('sy-show');
      $('#sy-reset-email').focus();
      return;
    }

    $('#sy-login-form-view').addClass('sy-show');
  }

  function showLoginMessage(message, type) {
    clearLoginMessage();

    if (!message) {
      return;
    }

    const messageClass = type === 'success' ? 'sy-msg-success' : 'sy-msg-error';
    $('<div>')
      .addClass('sy-msg sy-login-client-msg ' + messageClass)
      .text(message)
      .insertBefore('#sy-login-form');
  }

  function clearLoginMessage() {
    $('.sy-login-client-msg').remove();
  }

  function restoreRememberedEmail() {
    try {
      const rememberedEmail = localStorage.getItem(REMEMBER_EMAIL_KEY) || '';

      if (rememberedEmail.length === 0) {
        return;
      }

      $('#sy-login-email').val(rememberedEmail);
      $('#sy-remember-email').prop('checked', true);
    } catch (error) {
      $('#sy-remember-email').prop('checked', false);
    }
  }

  function saveRememberedEmail() {
    try {
      if ($('#sy-remember-email').is(':checked')) {
        localStorage.setItem(REMEMBER_EMAIL_KEY, $('#sy-login-email').val().trim());
        return;
      }

      localStorage.removeItem(REMEMBER_EMAIL_KEY);
    } catch (error) {
      return;
    }
  }

  function openSignupPane() {
    resetSignupFlow();

    $('.sy-tab').removeClass('sy-on');
    $('.sy-pane').removeClass('sy-on');

    $('.sy-tab[data-target="sy-signup"]').addClass('sy-on');
    $('#sy-signup').addClass('sy-on');
    showSignupStep('email');
    pushSignupHistoryState();
  }

  function showSignupPaneAfterServerError() {
    $('.sy-tab').removeClass('sy-on');
    $('.sy-pane').removeClass('sy-on');

    $('.sy-tab[data-target="sy-signup"]').addClass('sy-on');
    $('#sy-signup').addClass('sy-on');
    showSignupStep('email');
    $('#sy-signup-email').focus();
  }

  function pushSignupHistoryState() {
    if (!window.history || !window.history.pushState || signupHistoryActive) {
      return;
    }

    window.history.pushState({ syView: 'signup' }, '', window.location.href);
    signupHistoryActive = true;
  }

  function confirmSignupLeave() {
    if (hasSignupProgress() && !window.confirm(SIGNUP_LEAVE_MESSAGE)) {
      return false;
    }

    resetSignupFlow();
    showLoginPane();

    if (window.history && window.history.replaceState) {
      window.history.replaceState({ syView: 'login' }, '', window.location.href);
    }

    signupHistoryActive = false;
    return true;
  }

  function showLoginPane() {
    $('.sy-pane').removeClass('sy-on');
    $('#sy-login').addClass('sy-on');
    showAuthView('login');
  }

  function hasSignupProgress() {
    if (!$('#sy-signup').hasClass('sy-on')) {
      return false;
    }

    if (!$('#sy-signup-step-email').hasClass('sy-show')) {
      return true;
    }

    if (emailChecked || emailCodeSent || emailVerified || checkedEmail.length > 0 || verifiedEmail.length > 0) {
      return true;
    }

    let hasValue = false;
    $('#sy-signup-form')
      .find('input[type="text"], input[type="email"], input[type="password"], textarea')
      .each(function () {
        if ($(this).val().trim().length > 0) {
          hasValue = true;
          return false;
        }
        return true;
      });

    if (hasValue) {
      return true;
    }

    return $('#sy-signup-form').find('input[type="checkbox"]:checked').length > 0;
  }

  function resetSignupFlow() {
    stopEmailTimer();

    emailChecked = false;
    emailCodeSent = false;
    emailVerified = false;
    checkedEmail = '';
    verifiedEmail = '';
    remainSeconds = 0;
	phoneChecked = false;
	checkedPhone = '';

    $('#sy-signup-form')
      .find('input[type="text"], input[type="email"], input[type="password"], textarea')
      .val('')
      .removeClass('sy-input-error');

    $('#sy-signup-form')
      .find('input[type="checkbox"]')
      .prop('checked', false);

    $('.sy-terms').removeClass('sy-input-error');
    $('#sy-signup-form').find('.sy-field-error').remove();
    $('#sy-email-check-msg').removeClass('sy-ok-text sy-err-text').text('');
    $('#sy-email-code-msg').removeClass('sy-ok-text sy-err-text').text('메일로 전송된 인증번호를 입력해주세요.');
    $('#sy-email-code-msg').closest('.sy-code-info').removeClass('sy-show');
    $('#sy-email-timer').text('05:00');
    $('#sy-btn-check-email').text(EMAIL_BUTTON_CHECK).prop('disabled', false);
    $('#sy-btn-verify-email').prop('disabled', false);
    $('#sy-email-code').prop('readonly', false);
	$('#sy-phone-check-msg').removeClass('sy-ok-text sy-err-text').text('');
	$('#sy-check-phone').prop('disabled', false);

    showSignupStep('email');
  }

  function showSignupStep(stepName) {
    $('.sy-signup-step').removeClass('sy-show');
    $('.sy-step-dot').removeClass('sy-active sy-done');
    $('.sy-step-labels span').removeClass('sy-active');

    const steps = ['email', 'account', 'profile'];
    const currentIndex = steps.indexOf(stepName);

    $('#sy-signup-step-' + stepName).addClass('sy-show');
    $('[data-step-label="' + stepName + '"]').addClass('sy-active');

    steps.forEach(function (step, index) {
      const $dot = $('[data-step-dot="' + step + '"]');

      if (index < currentIndex) {
        $dot.addClass('sy-done').text('✓');
        return;
      }

      $dot.text(index + 1);

      if (index === currentIndex) {
        $dot.addClass('sy-active');
      }
    });
  }

  function requireValue($input, value, message) {
    if (value.length === 0) {
      markInvalid($input, message);
      return false;
    }
    return true;
  }

  function validateEmail($input, email, message) {
    if (!isEmailFormat(email)) {
      markInvalid($input, message);
      return false;
    }
    return true;
  }

  function validatePhone($input, phone, message) {
    if (!/^01[0-9]{8,9}$/.test(phone)) {
      markInvalid($input, message);
      return false;
    }
    return true;
  }

  function validateBirth($input, birthText, message) {
    if (!/^[0-9]{8}$/.test(birthText)) {
      markInvalid($input, message);
      return false;
    }
    return true;
  }

  function validatePassword($input, passwd, message) {
    if (!PASSWORD_PATTERN.test(passwd)) {
      markInvalid($input, message);
      return false;
    }
    return true;
  }

  function markInvalid($input, message) {
    $input.addClass('sy-input-error').focus();

    if (!message) {
      return;
    }

    const $field = $input.hasClass('sy-terms') ? $input : $input.closest('.sy-field');
    const $status = $field.children('.sy-check-msg').first();

    if ($status.length > 0) {
      setStateMessage($status, message, 'error');
      return;
    }

    let $error = $field.children('.sy-field-error');

    if ($error.length === 0) {
      $error = $('<div class="sy-field-error"></div>');
      $field.append($error);
    }

    $error.text(message);
  }

  function clearInvalid($input) {
    $input.removeClass('sy-input-error');

    const $field = $input.hasClass('sy-terms') ? $input : $input.closest('.sy-field');
    $field.children('.sy-field-error').remove();
  }

  function startEmailTimer(seconds) {
    stopEmailTimer();

    remainSeconds = seconds;
    renderEmailTimer();

    timerId = setInterval(function () {
      remainSeconds -= 1;
      renderEmailTimer();

      if (remainSeconds <= 0) {
        expireEmailVerification();
      }
    }, 1000);
  }

  function stopEmailTimer() {
    if (timerId !== null) {
      clearInterval(timerId);
      timerId = null;
    }
  }

  function renderEmailTimer() {
    const minutes = String(Math.floor(remainSeconds / 60)).padStart(2, '0');
    const seconds = String(remainSeconds % 60).padStart(2, '0');
    $('#sy-email-timer').text(minutes + ':' + seconds);
  }

  function startResetTimer(seconds) {
    stopResetTimer();

    resetRemainSeconds = seconds;
    renderResetTimer();

    resetTimerId = setInterval(function () {
      resetRemainSeconds -= 1;
      renderResetTimer();

      if (resetRemainSeconds <= 0) {
        stopResetTimer();
        resetPasswordVerified = false;
        $('#sy-reset-code').addClass('sy-input-error');
        setResetCodeMessage('인증 시간이 만료되었습니다. 인증번호를 다시 전송해주세요.', 'error');
      }
    }, 1000);
  }

  function stopResetTimer() {
    if (resetTimerId !== null) {
      clearInterval(resetTimerId);
      resetTimerId = null;
    }
  }

  function renderResetTimer() {
    const minutes = String(Math.floor(resetRemainSeconds / 60)).padStart(2, '0');
    const seconds = String(resetRemainSeconds % 60).padStart(2, '0');
    $('#sy-reset-timer').text(minutes + ':' + seconds);
  }

  function setEmailMessage(message, type) {
    setStateMessage($('#sy-email-check-msg'), message, type);
  }

  function setFindEmailMessage(message, type) {
    setStateMessage($('#sy-find-email-msg'), message, type);
  }

  function setResetEmailMessage(message, type) {
    setStateMessage($('#sy-reset-email-msg'), message, type);
  }

  function setResetCodeMessage(message, type) {
    setStateMessage($('#sy-reset-code-msg'), message, type);
  }

  function setSignupCodeError(message) {
    $('#sy-email-code-msg').closest('.sy-code-info').addClass('sy-show');
    $('#sy-email-code').addClass('sy-input-error').focus();
    clearSignupCodeFieldError();
    setCodeMessage(message, 'error');
  }

  function setSignupCodeOk(message) {
    $('#sy-email-code').removeClass('sy-input-error');
    clearSignupCodeFieldError();
    setCodeMessage(message, 'ok');
  }

  function clearSignupCodeState() {
    $('#sy-email-code').removeClass('sy-input-error');
    clearSignupCodeFieldError();
  }

  function clearSignupCodeFieldError() {
    $('#sy-email-code').closest('.sy-field').children('.sy-field-error').remove();
  }

  function setCodeMessage(message, type) {
    setStateMessage($('#sy-email-code-msg'), message, type);
  }

  function setStateMessage($message, message, type) {
    $message
      .removeClass('sy-ok-text sy-err-text')
      .text(message);

    if (type === 'ok') {
      $message.addClass('sy-ok-text');
    }

    if (type === 'error') {
      $message.addClass('sy-err-text');
    }
  }

  function isEmailFormat(email) {
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
  }
});
