import bcrypt

def hash_password(password: str) -> str:
    """Hash a password string using bcrypt and return the decoded string."""
    if not password:
        raise ValueError("Password cannot be empty")
    salt = bcrypt.gensalt()
    hashed_bytes = bcrypt.hashpw(password.encode('utf-8'), salt)
    return hashed_bytes.decode('utf-8')

def verify_password(password: str, hashed_password: str) -> bool:
    """Verify a plain-text password against a bcrypt hash."""
    if not password or not hashed_password:
        return False
    try:
        password_bytes = password.encode('utf-8')
        hashed_bytes = hashed_password.encode('utf-8')
        return bcrypt.checkpw(password_bytes, hashed_bytes)
    except Exception:
        return False
