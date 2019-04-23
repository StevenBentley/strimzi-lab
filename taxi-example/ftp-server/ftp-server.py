import logging

from pyftpdlib.authorizers import DummyAuthorizer
from pyftpdlib.handlers import FTPHandler
from pyftpdlib.servers import FTPServer

PREFIX = '[%(levelname)1.1s %(asctime)s]'
TIME_FORMAT = "%Y-%m-%d %H:%M:%S"
FORMAT = PREFIX + ' %(message)s'

#logging.basicConfig(level=logging.DEBUG, format=FORMAT)

USER = "strimzi"
PASS = "strimzi"
PATH = "./taxi-data"

authorizer = DummyAuthorizer()
authorizer.add_user(USER, PASS, PATH)

handler = FTPHandler
handler.authorizer = authorizer
handler.timeout = None
handler.permit_foreign_addresses = True

server = FTPServer(("", 21), handler)
server.serve_forever()



