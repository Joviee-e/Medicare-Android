from flask_jwt_extended import create_access_token, create_refresh_token
from datetime import timedelta

def generate_tokens(user_id: str, role: str):
    """
    Generate JWT access and refresh tokens.
    Access token expires in 1 day; refresh token expires in 30 days.
    """
    identity = str(user_id)
    additional_claims = {"role": role}
    
    access_token = create_access_token(
        identity=identity, 
        additional_claims=additional_claims,
        expires_delta=timedelta(days=1)
    )
    refresh_token = create_refresh_token(
        identity=identity,
        expires_delta=timedelta(days=30)
    )
    return access_token, refresh_token
