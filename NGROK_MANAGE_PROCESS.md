# 🔧 How to Manage ngrok Process

## 🔍 Find Running ngrok Process

### Method 1: Check Task Manager
1. Press `Ctrl + Shift + Esc` to open Task Manager
2. Look for "ngrok" in the list
3. Right-click → End Task

### Method 2: Use PowerShell (Quick)

**Find ngrok:**
```powershell
Get-Process -Name ngrok
```

**Stop ngrok:**
```powershell
Stop-Process -Name ngrok -Force
```

**Or kill all ngrok processes:**
```powershell
Get-Process -Name ngrok | Stop-Process -Force
```

---

## ✅ Start New ngrok

After stopping the old one:

```powershell
ngrok http 8080
```

---

## 🔍 Check if ngrok is Running

**Check processes:**
```powershell
Get-Process -Name ngrok -ErrorAction SilentlyContinue
```

**If nothing shows, ngrok is not running.**

**If you see output, ngrok is running.**

---

## 💡 Quick Commands

**Stop ngrok:**
```powershell
Stop-Process -Name ngrok -Force
```

**Start ngrok:**
```powershell
ngrok http 8080
```

**Check status:**
```powershell
Get-Process -Name ngrok -ErrorAction SilentlyContinue
```

---

## 🎯 Typical Workflow

1. **Stop old ngrok** (if running):
   ```powershell
   Stop-Process -Name ngrok -Force
   ```

2. **Start new ngrok**:
   ```powershell
   ngrok http 8080
   ```

3. **Copy the HTTPS URL** from the output

4. **Keep the terminal open** (ngrok needs to keep running)

---

That's it! Now you can manage ngrok easily! 🚀

