import re
from datetime import datetime

def is_valid_email(email: str) -> bool:
    """Validate email using a standard regular expression pattern."""
    if not email:
        return False
    pattern = r'^[\w\.-]+@[\w\.-]+\.\w+$'
    return bool(re.match(pattern, email))

def is_valid_password(password: str) -> bool:
    """Validate password strength (minimum 6 characters)."""
    return bool(password and len(password) >= 6)

def parse_date(date_str: str) -> datetime:
    """
    Parse a date string. Supports:
    1. YYYY-MM-DD
    2. DD-MM-YYYY
    Returns datetime object or raises ValueError.
    """
    if not date_str:
        raise ValueError("Date string cannot be empty")
    
    for fmt in ('%Y-%m-%d', '%d-%m-%Y'):
        try:
            return datetime.strptime(date_str, fmt)
        except ValueError:
            continue
    raise ValueError(f"Date '{date_str}' is not in YYYY-MM-DD or DD-MM-YYYY format")

def parse_time(time_str: str) -> datetime:
    """
    Parse a time string. Supports:
    1. HH:MM AM/PM (e.g. 08:00 AM)
    2. H:MM AM/PM (e.g. 8:00 AM)
    3. HH:MM (24h format, e.g. 20:00)
    Returns datetime object (using dummy date) or raises ValueError.
    """
    if not time_str:
        raise ValueError("Time string cannot be empty")
    
    # Strip any extra spaces
    time_str = time_str.strip()
    
    for fmt in ('%I:%M %p', '%I:%M%p', '%H:%M'):
        try:
            return datetime.strptime(time_str, fmt)
        except ValueError:
            continue
    raise ValueError(f"Time '{time_str}' is not in HH:MM AM/PM or HH:MM format")

def is_valid_object_id(oid: str) -> bool:
    """Check if a string is a valid MongoDB ObjectId (24 hex characters)."""
    if not oid:
        return False
    return bool(re.match(r'^[0-9a-fA-F]{24}$', oid))
