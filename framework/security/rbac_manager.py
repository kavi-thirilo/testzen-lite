#!/usr/bin/env python3
"""
Role-Based Access Control (RBAC) and User Management for TestZen Framework
Provides authentication, authorization, and user management capabilities
"""

import hashlib
import jwt
import json
import sqlite3
import secrets
import time
from datetime import datetime, timedelta
from typing import Dict, List, Optional, Any
from dataclasses import dataclass
from enum import Enum
import logging
from pathlib import Path

class Role(Enum):
    ADMIN = "admin"
    READ_WRITE = "read_write"
    READ_ONLY = "read_only"
    GUEST = "guest"

class Permission(Enum):
    # Test Management
    CREATE_TEST = "create_test"
    EDIT_TEST = "edit_test"
    DELETE_TEST = "delete_test"
    EXECUTE_TEST = "execute_test"
    VIEW_TEST = "view_test"

    # Report Management
    VIEW_REPORT = "view_report"
    EXPORT_REPORT = "export_report"
    DELETE_REPORT = "delete_report"

    # Configuration Management
    EDIT_CONFIG = "edit_config"
    VIEW_CONFIG = "view_config"

    # User Management
    MANAGE_USERS = "manage_users"
    MANAGE_ROLES = "manage_roles"

    # System Management
    SYSTEM_ADMIN = "system_admin"
    VIEW_LOGS = "view_logs"
    MANAGE_INTEGRATIONS = "manage_integrations"

@dataclass
class User:
    username: str
    email: str
    role: Role
    created_at: datetime
    last_login: Optional[datetime] = None
    is_active: bool = True
    is_2fa_enabled: bool = False
    api_key: Optional[str] = None

@dataclass
class Session:
    session_id: str
    user_id: str
    created_at: datetime
    expires_at: datetime
    ip_address: str
    user_agent: str

class RolePermissionMapping:
    """Define permissions for each role"""

    PERMISSIONS = {
        Role.ADMIN: [
            Permission.CREATE_TEST,
            Permission.EDIT_TEST,
            Permission.DELETE_TEST,
            Permission.EXECUTE_TEST,
            Permission.VIEW_TEST,
            Permission.VIEW_REPORT,
            Permission.EXPORT_REPORT,
            Permission.DELETE_REPORT,
            Permission.EDIT_CONFIG,
            Permission.VIEW_CONFIG,
            Permission.MANAGE_USERS,
            Permission.MANAGE_ROLES,
            Permission.SYSTEM_ADMIN,
            Permission.VIEW_LOGS,
            Permission.MANAGE_INTEGRATIONS
        ],
        Role.READ_WRITE: [
            Permission.CREATE_TEST,
            Permission.EDIT_TEST,
            Permission.EXECUTE_TEST,
            Permission.VIEW_TEST,
            Permission.VIEW_REPORT,
            Permission.EXPORT_REPORT,
            Permission.VIEW_CONFIG
        ],
        Role.READ_ONLY: [
            Permission.VIEW_TEST,
            Permission.VIEW_REPORT,
            Permission.VIEW_CONFIG
        ],
        Role.GUEST: [
            Permission.VIEW_TEST,
            Permission.VIEW_REPORT
        ]
    }

    @classmethod
    def has_permission(cls, role: Role, permission: Permission) -> bool:
        """Check if role has specific permission"""
        return permission in cls.PERMISSIONS.get(role, [])

class AuthenticationManager:
    """Handle user authentication"""

    def __init__(self, secret_key: str = None):
        self.secret_key = secret_key or secrets.token_hex(32)
        self.logger = logging.getLogger(__name__)

    def hash_password(self, password: str) -> str:
        """Hash password using SHA-256"""
        salt = secrets.token_hex(16)
        password_hash = hashlib.sha256(f"{password}{salt}".encode()).hexdigest()
        return f"{salt}${password_hash}"

    def verify_password(self, password: str, hashed: str) -> bool:
        """Verify password against hash"""
        try:
            salt, password_hash = hashed.split('$')
            return hashlib.sha256(f"{password}{salt}".encode()).hexdigest() == password_hash
        except:
            return False

    def generate_token(self, user: User, expires_in: int = 3600) -> str:
        """Generate JWT token for user"""
        payload = {
            'username': user.username,
            'email': user.email,
            'role': user.role.value,
            'exp': datetime.utcnow() + timedelta(seconds=expires_in),
            'iat': datetime.utcnow()
        }
        return jwt.encode(payload, self.secret_key, algorithm='HS256')

    def verify_token(self, token: str) -> Optional[Dict[str, Any]]:
        """Verify and decode JWT token"""
        try:
            payload = jwt.decode(token, self.secret_key, algorithms=['HS256'])
            return payload
        except jwt.ExpiredSignatureError:
            self.logger.warning("Token expired")
            return None
        except jwt.InvalidTokenError:
            self.logger.warning("Invalid token")
            return None

    def generate_2fa_secret(self) -> str:
        """Generate 2FA secret for user"""
        import pyotp
        return pyotp.random_base32()

    def verify_2fa_token(self, secret: str, token: str) -> bool:
        """Verify 2FA token"""
        import pyotp
        totp = pyotp.TOTP(secret)
        return totp.verify(token, valid_window=1)

class UserManager:
    """Manage users and their permissions"""

    def __init__(self, db_path: str = "data/users.db"):
        self.db_path = Path(db_path)
        self.db_path.parent.mkdir(parents=True, exist_ok=True)
        self.auth_manager = AuthenticationManager()
        self.logger = logging.getLogger(__name__)
        self._init_database()

    def _init_database(self):
        """Initialize user database"""
        conn = sqlite3.connect(self.db_path)
        cursor = conn.cursor()

        # Users table
        cursor.execute("""
            CREATE TABLE IF NOT EXISTS users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                username TEXT UNIQUE NOT NULL,
                email TEXT UNIQUE NOT NULL,
                password_hash TEXT NOT NULL,
                role TEXT NOT NULL,
                is_active BOOLEAN DEFAULT 1,
                is_2fa_enabled BOOLEAN DEFAULT 0,
                secret_2fa TEXT,
                api_key TEXT UNIQUE,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                last_login TIMESTAMP
            )
        """)

        # Sessions table
        cursor.execute("""
            CREATE TABLE IF NOT EXISTS sessions (
                session_id TEXT PRIMARY KEY,
                user_id INTEGER,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                expires_at TIMESTAMP,
                ip_address TEXT,
                user_agent TEXT,
                FOREIGN KEY (user_id) REFERENCES users(id)
            )
        """)

        # Audit log table
        cursor.execute("""
            CREATE TABLE IF NOT EXISTS audit_log (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER,
                action TEXT,
                resource TEXT,
                details TEXT,
                timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (user_id) REFERENCES users(id)
            )
        """)

        # Create default admin user if not exists
        cursor.execute("SELECT COUNT(*) FROM users WHERE role = ?", (Role.ADMIN.value,))
        if cursor.fetchone()[0] == 0:
            self.create_user("admin", "admin@testzen.local", "admin123", Role.ADMIN)

        conn.commit()
        conn.close()

    def create_user(self, username: str, email: str, password: str, role: Role) -> bool:
        """Create new user"""
        conn = sqlite3.connect(self.db_path)
        cursor = conn.cursor()

        try:
            password_hash = self.auth_manager.hash_password(password)
            api_key = secrets.token_urlsafe(32)

            cursor.execute("""
                INSERT INTO users (username, email, password_hash, role, api_key)
                VALUES (?, ?, ?, ?, ?)
            """, (username, email, password_hash, role.value, api_key))

            conn.commit()
            self.logger.info(f"User created: {username}")
            return True

        except sqlite3.IntegrityError:
            self.logger.error(f"User already exists: {username}")
            return False
        finally:
            conn.close()

    def authenticate(self, username: str, password: str, totp_token: str = None) -> Optional[User]:
        """Authenticate user with credentials"""
        conn = sqlite3.connect(self.db_path)
        cursor = conn.cursor()

        cursor.execute("""
            SELECT id, username, email, password_hash, role, is_active,
                   is_2fa_enabled, secret_2fa, api_key, created_at, last_login
            FROM users WHERE username = ?
        """, (username,))

        row = cursor.fetchone()
        if not row:
            return None

        user_id, username, email, password_hash, role, is_active, \
        is_2fa_enabled, secret_2fa, api_key, created_at, last_login = row

        # Verify password
        if not self.auth_manager.verify_password(password, password_hash):
            self.log_audit(user_id, "LOGIN_FAILED", "authentication", "Invalid password")
            return None

        # Check if user is active
        if not is_active:
            self.log_audit(user_id, "LOGIN_FAILED", "authentication", "Account inactive")
            return None

        # Verify 2FA if enabled
        if is_2fa_enabled:
            if not totp_token or not self.auth_manager.verify_2fa_token(secret_2fa, totp_token):
                self.log_audit(user_id, "LOGIN_FAILED", "authentication", "Invalid 2FA token")
                return None

        # Update last login
        cursor.execute("UPDATE users SET last_login = ? WHERE id = ?",
                      (datetime.now(), user_id))
        conn.commit()
        conn.close()

        user = User(
            username=username,
            email=email,
            role=Role(role),
            created_at=datetime.fromisoformat(created_at),
            last_login=datetime.now(),
            is_active=is_active,
            is_2fa_enabled=is_2fa_enabled,
            api_key=api_key
        )

        self.log_audit(user_id, "LOGIN_SUCCESS", "authentication", "User logged in")
        return user

    def create_session(self, user: User, ip_address: str, user_agent: str) -> str:
        """Create user session"""
        conn = sqlite3.connect(self.db_path)
        cursor = conn.cursor()

        session_id = secrets.token_urlsafe(32)
        expires_at = datetime.now() + timedelta(hours=24)

        cursor.execute("""
            INSERT INTO sessions (session_id, user_id, expires_at, ip_address, user_agent)
            VALUES (?, (SELECT id FROM users WHERE username = ?), ?, ?, ?)
        """, (session_id, user.username, expires_at, ip_address, user_agent))

        conn.commit()
        conn.close()

        return session_id

    def verify_session(self, session_id: str) -> Optional[User]:
        """Verify session and return user"""
        conn = sqlite3.connect(self.db_path)
        cursor = conn.cursor()

        cursor.execute("""
            SELECT u.username, u.email, u.role, u.is_active, u.created_at, u.api_key
            FROM sessions s
            JOIN users u ON s.user_id = u.id
            WHERE s.session_id = ? AND s.expires_at > ?
        """, (session_id, datetime.now()))

        row = cursor.fetchone()
        conn.close()

        if not row:
            return None

        username, email, role, is_active, created_at, api_key = row

        if not is_active:
            return None

        return User(
            username=username,
            email=email,
            role=Role(role),
            created_at=datetime.fromisoformat(created_at),
            is_active=is_active,
            api_key=api_key
        )

    def update_user_role(self, username: str, new_role: Role, admin_user: User) -> bool:
        """Update user role (admin only)"""
        if not RolePermissionMapping.has_permission(admin_user.role, Permission.MANAGE_USERS):
            self.logger.warning(f"Unauthorized role change attempt by {admin_user.username}")
            return False

        conn = sqlite3.connect(self.db_path)
        cursor = conn.cursor()

        cursor.execute("UPDATE users SET role = ? WHERE username = ?",
                      (new_role.value, username))

        affected = cursor.rowcount
        conn.commit()
        conn.close()

        if affected > 0:
            self.log_audit_by_username(admin_user.username, "UPDATE_ROLE", username,
                                      f"Role changed to {new_role.value}")
            return True
        return False

    def enable_2fa(self, username: str) -> str:
        """Enable 2FA for user and return secret"""
        secret = self.auth_manager.generate_2fa_secret()

        conn = sqlite3.connect(self.db_path)
        cursor = conn.cursor()

        cursor.execute("""
            UPDATE users SET is_2fa_enabled = 1, secret_2fa = ?
            WHERE username = ?
        """, (secret, username))

        conn.commit()
        conn.close()

        self.log_audit_by_username(username, "ENABLE_2FA", "security", "2FA enabled")
        return secret

    def log_audit(self, user_id: int, action: str, resource: str, details: str):
        """Log audit trail"""
        conn = sqlite3.connect(self.db_path)
        cursor = conn.cursor()

        cursor.execute("""
            INSERT INTO audit_log (user_id, action, resource, details)
            VALUES (?, ?, ?, ?)
        """, (user_id, action, resource, details))

        conn.commit()
        conn.close()

    def log_audit_by_username(self, username: str, action: str, resource: str, details: str):
        """Log audit trail by username"""
        conn = sqlite3.connect(self.db_path)
        cursor = conn.cursor()

        cursor.execute("SELECT id FROM users WHERE username = ?", (username,))
        row = cursor.fetchone()
        if row:
            self.log_audit(row[0], action, resource, details)

        conn.close()

    def get_audit_log(self, limit: int = 100) -> List[Dict[str, Any]]:
        """Get audit log entries"""
        conn = sqlite3.connect(self.db_path)
        cursor = conn.cursor()

        cursor.execute("""
            SELECT u.username, a.action, a.resource, a.details, a.timestamp
            FROM audit_log a
            JOIN users u ON a.user_id = u.id
            ORDER BY a.timestamp DESC
            LIMIT ?
        """, (limit,))

        logs = []
        for row in cursor.fetchall():
            logs.append({
                'username': row[0],
                'action': row[1],
                'resource': row[2],
                'details': row[3],
                'timestamp': row[4]
            })

        conn.close()
        return logs

class AuthorizationDecorator:
    """Decorator for method authorization"""

    @staticmethod
    def require_permission(permission: Permission):
        """Decorator to require specific permission"""
        def decorator(func):
            def wrapper(self, user: User, *args, **kwargs):
                if not RolePermissionMapping.has_permission(user.role, permission):
                    raise PermissionError(f"User {user.username} lacks permission: {permission.value}")
                return func(self, user, *args, **kwargs)
            return wrapper
        return decorator

    @staticmethod
    def require_role(role: Role):
        """Decorator to require specific role"""
        def decorator(func):
            def wrapper(self, user: User, *args, **kwargs):
                if user.role != role:
                    raise PermissionError(f"User {user.username} requires role: {role.value}")
                return func(self, user, *args, **kwargs)
            return wrapper
        return decorator