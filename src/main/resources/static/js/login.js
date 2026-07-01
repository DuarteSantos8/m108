/* OWASP Demo-Lab — Login UX helpers.
   Loaded as an external file so it complies with the strict CSP
   (script-src 'self'; no inline handlers). Pure progressive enhancement:
   the form works fully without this script. */
(function () {
  "use strict";

  /* --- Show / hide password ------------------------------------------- */
  var toggle = document.getElementById("toggle-pw");
  var pw = document.getElementById("password");
  if (toggle && pw) {
    toggle.addEventListener("click", function () {
      var reveal = pw.type === "password";
      pw.type = reveal ? "text" : "password";
      toggle.classList.toggle("revealed", reveal);
      toggle.setAttribute("aria-pressed", String(reveal));
      toggle.setAttribute("aria-label", reveal ? "Passwort verbergen" : "Passwort anzeigen");
      pw.focus({ preventScroll: true });
    });
  }

  /* --- Caps Lock hint -------------------------------------------------- */
  var capsHint = document.getElementById("caps-hint");
  if (capsHint && pw) {
    var updateCaps = function (e) {
      if (typeof e.getModifierState !== "function") return;
      capsHint.classList.toggle("show", e.getModifierState("CapsLock"));
    };
    pw.addEventListener("keydown", updateCaps);
    pw.addEventListener("keyup", updateCaps);
    pw.addEventListener("blur", function () { capsHint.classList.remove("show"); });
  }

  /* --- CAPTCHA refresh (new code without full page reload) ------------- */
  var refresh = document.getElementById("captcha-refresh");
  var img = document.getElementById("captcha-img");
  if (refresh && img) {
    refresh.addEventListener("click", function () {
      refresh.classList.remove("spin");
      // restart the spin animation
      void refresh.offsetWidth;
      refresh.classList.add("spin");
      img.src = "/captcha.png?t=" + Date.now();
      var cap = document.getElementById("captcha");
      if (cap) { cap.value = ""; cap.focus({ preventScroll: true }); }
    });
  }

  /* --- Demo quick-fill ------------------------------------------------- */
  var user = document.getElementById("username");
  document.querySelectorAll(".chip[data-user]").forEach(function (chip) {
    chip.addEventListener("click", function () {
      if (user) user.value = chip.getAttribute("data-user");
      if (pw) pw.value = chip.getAttribute("data-pass");
      var cap = document.getElementById("captcha");
      if (cap) cap.focus({ preventScroll: true });
    });
  });
})();
