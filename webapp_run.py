from webapp.app import app

logo = r"""
  ____   ____ __  __  ___      _   ____            _           _   
 | __ ) / ___|  \/  |/ _ \    | | |  _ \ _ __ ___ (_) ___  ___| |_ 
 |  _ \| |   | |\/| | | | |_  | | | |_) | '__/ _ \| |/ _ \/ __| __|
 | |_) | |___| |  | | |_| | |_| | |  __/| | | (_) | |  __| (__| |_ 
 |____/ \____|_|  |_|\___/ \___/  |_|   |_|  \____/ |\___|\___|\__|
                                                |__/               
 Developed by SleepingCui    https://github.com/SleepingCui/BCMOJ/

 
"""
if __name__ == '__main__':
    print(logo)
    app.run()