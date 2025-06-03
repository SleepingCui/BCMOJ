from webapp.app import app, log_route_context
from werkzeug.serving import WSGIRequestHandler
import logging

class CustomRequestHandler(WSGIRequestHandler):
    def log(self, type, message, *args):
        if args:
            message = message % args
        logger = logging.getLogger('werkzeug')
        level = {
            'info': logging.INFO,
            'warning': logging.WARNING,
            'error': logging.ERROR,
        }.get(type, logging.INFO)
        client_ip = self.client_address[0]
        route_name = log_route_context.get('werkzeug') if log_route_context.get() == 'main' else log_route_context.get()
        logger.log(level, '[%s] %s - %s', route_name, client_ip, message)



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
    app.run(request_handler=CustomRequestHandler)