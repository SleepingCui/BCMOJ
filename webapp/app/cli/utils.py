logo = r"""
  ____   ____ __  __  ___      _   ____            _           _    
 | __ ) / ___|  \/  |/ _ \    | | |  _ \ _ __ ___ (_) ___  ___| |_  
 |  _ \| |   | |\/| | | | |_  | | | |_) | '__/ _ \| |/ _ \/ __| __| 
 | |_) | |___| |  | | |_| | |_| | |  __/| | | (_) | |  __| (__| |_ 
 |____/ \____|_|  |_|\___/ \___/  |_|   |_|  \____/ |\___|\___|\__|
                                                 |__/              
  Developed by SleepingCui    https://github.com/SleepingCui/BCMOJ/
"""

def check_database_exists():
    try:
        from app.core.config import get_config
        from sqlalchemy import create_engine, text
        
        config = get_config()
        raw_db_config = config['db_config']
        base_uri = f"mysql+pymysql://{raw_db_config['db_user']}:{raw_db_config['db_password']}@" \
                   f"{raw_db_config['db_host']}:{raw_db_config['db_port']}"
        
        engine = create_engine(base_uri, echo=False)
        
        with engine.connect() as conn:
            result = conn.execute(text(
                f"SELECT SCHEMA_NAME FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME = '{raw_db_config['db_name']}'"
            ))
            db_exists = result.fetchone() is not None
            
            if not db_exists:
                return False, f"Database '{raw_db_config['db_name']}' does not exist"
            full_uri = f"{base_uri}/{raw_db_config['db_name']}"
            db_engine = create_engine(full_uri, echo=False)
            
            with db_engine.connect() as db_conn:
                result = db_conn.execute(text("SHOW TABLES"))
                tables = result.fetchall()
                
                if not tables:
                    return False, f"Database '{raw_db_config['db_name']}' exists but has no tables"
                
                return True, f"Database '{raw_db_config['db_name']}' exists with {len(tables)} tables"
                
    except Exception as e:
        return False, f"Error checking database: {e}"